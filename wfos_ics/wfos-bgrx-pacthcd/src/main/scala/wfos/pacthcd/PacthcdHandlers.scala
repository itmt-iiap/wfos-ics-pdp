package wfos.pacthcd

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse._
import csw.params.core.models.{Id}
import csw.params.commands.CommandIssue.{ParameterValueOutOfRangeIssue, UnsupportedCommandIssue}
import csw.params.commands.{ControlCommand, Setup}

import csw.time.core.models.UTCTime

import scala.concurrent.{ExecutionContextExecutor}
import wfos.pacthcd.PactInfo
import csw.params.events.{SystemEvent, EventName}

class PacthcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val prefix                        = cswCtx.componentInfo.prefix
  private val publisher                     = eventService.defaultPublisher

  override def initialize(): Unit = {
    log.info(s"Initializing $prefix")
    log.info(s"PactHcd : Checking if $prefix is at its current position")

    log.info(s"Current Position - ${PactInfo.currentPosition.head}")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    Accepted(runId);
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    log.info(s"PactHcd : Handling command with runId - $runId")
    controlCommand match {
      case setup: Setup => onSetup(runId, setup)
      case _            => Invalid(runId, UnsupportedCommandIssue("PactHcd : Inavlid Command received"))
    }
  }

  private def onSetup(runId: Id, setup: Setup): SubmitResponse = {
    println(s"PactHcd : Received command - OnSubmit - Getting called")
    log.info(s"PactHcd : Executing the received command with runId - $runId")

    val step  = PactInfo.movementStep.head
    val min   = PactInfo.minExtension.head
    val max   = PactInfo.maxExtension.head
    val delay = 50 // ms per step (movement delay)

    def moveActuator(target: Double): Unit = {
      var pos = PactInfo.currentPosition.head
      while (Math.abs(pos - target) > step) {
        pos =
          if (pos < target) Math.min(pos + step, max)
          else Math.max(pos - step, min)

        PactInfo.currentPosition = PactInfo.currentPositionKey.set(pos)

        val message = s"PactHcd : Moving actuator to $pos mm"
        val event   = createMovementEvent(message)
        publisher.publish(event)

        Thread.sleep(delay)
      }

      // snap to final target
      PactInfo.currentPosition = PactInfo.currentPositionKey.set(target)
      log.info(s"PactHcd : Actuator reached target position ${PactInfo.currentPosition.head} mm")
    }

    val current = PactInfo.currentPosition.head
    log.info(s"PactHcd : Actuator is currently at $current mm")

    // 1. Move to in-position (500mm)
    moveActuator(500)

    // 2. Wait 2 seconds
    log.info("PactHcd : Waiting for 2 seconds at in-position before moving back")
    Thread.sleep(2000)

    // 3. Move back to out-position (0mm)
    moveActuator(0)

    Completed(runId)
  }

  private def createMovementEvent(message: String): SystemEvent = {
    // Create a SystemEvent representing the movement of the gripper
    SystemEvent(componentInfo.prefix, EventName("PactMovementEvent"))
      .madd(PactInfo.messageKey.set(message))
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
