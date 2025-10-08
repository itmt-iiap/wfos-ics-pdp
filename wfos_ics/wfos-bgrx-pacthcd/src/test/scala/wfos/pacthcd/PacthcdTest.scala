package wfos.pacthcd

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.testkit.scaladsl.CSWService.{LocationServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout

import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.{Setup, CommandName, CommandResponse, Observe}
import csw.command.client.CommandServiceFactory
import csw.params.core.generics.Parameter

import wfos.pacthcd.PactInfo

class PacthcdTest extends ScalaTestFrameworkTestKit(LocationServer, EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("PacthcdStandalone.conf"))
    LoggingSystemFactory.forTestingOnly()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  test("PactHCD should be locatable using Location Service") {
    val connection   = AkkaConnection(ComponentId(Prefix("wfos.pacthcd"), ComponentType.HCD))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    akkaLocation.connection shouldBe connection
  }

  test("PactHCD should accept Setup commands") {
    val connection   = AkkaConnection(ComponentId(Prefix("wfos.pacthcd"), ComponentType.HCD))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    val pactHcdCS = CommandServiceFactory.make(akkaLocation)

    // Create a simple Setup command
    val setupCmd: Setup = Setup(
      Prefix("wfos.pacthcd"),
      CommandName("setup"),
      Some(PactInfo.obsId)
    )

    val response = Await.result(pactHcdCS.submitAndWait(setupCmd), 5.seconds)

    response match {
      case _: CommandResponse.Completed =>
        succeed
    }
  }

  test("PactHCD should reject unsupported Observe command") {
    val connection   = AkkaConnection(ComponentId(Prefix("wfos.pacthcd"), ComponentType.HCD))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    val pactHcdCS = CommandServiceFactory.make(akkaLocation)

    val observeCmd: Observe = Observe(
      Prefix("wfos.pacthcd"),
      CommandName("observeGrating"),
      Some(PactInfo.obsId)
    )

    val response = Await.result(pactHcdCS.submitAndWait(observeCmd), 5.seconds)

    assert(
      response.isInstanceOf[CommandResponse.Invalid],
      s"Observe should not be accepted by PactHCD, but got: $response"
    )
  }

  implicit val timeout: Timeout = 10.seconds

  test("PactHCD should end in out-position (0mm) after moving in and back") {
    val connection   = AkkaConnection(ComponentId(Prefix("wfos.pacthcd"), ComponentType.HCD))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    val pactHcdCS = CommandServiceFactory.make(akkaLocation)

    val targetPosition: Parameter[Double] = PactInfo.targetPositionKey.set(500.0)
    val setupCmd: Setup = Setup(
      Prefix("wfos.pacthcd"),
      CommandName("setup"),
      Some(PactInfo.obsId)
    ).madd(targetPosition)

    Await.result(pactHcdCS.submitAndWait(setupCmd), 15.seconds)

    // Only check final state (0mm)
    assert(
      PactInfo.currentPosition.head == 0.0,
      s"Rod did not end at out-position (0mm), actual=${PactInfo.currentPosition.head}"
    )
  }
}
