package wfos.pacthcd

import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.ArrayData
import csw.params.core.models.ObsId

object PactInfo {

  // Physical movement constraints
  val maxExtensionKey: Key[Double]    = KeyType.DoubleKey.make("maxExtension")
  val maxExtension: Parameter[Double] = maxExtensionKey.set(500.0)

  val minExtensionKey: Key[Double]    = KeyType.DoubleKey.make("minExtension")
  val minExtension: Parameter[Double] = minExtensionKey.set(0.0)

  // Steps
  val movementStepKey: Key[Double]    = KeyType.DoubleKey.make("movementStep")
  val movementStep: Parameter[Double] = movementStepKey.set(50.0)

  // Position keys
  val targetPositionKey: Key[Double]    = KeyType.DoubleKey.make("targetPosition")
  val targetPosition: Parameter[Double] = targetPositionKey.set(500.0)

  // Current Position
  val currentPositionKey: Key[Double]    = KeyType.DoubleKey.make("currentPosition")
  var currentPosition: Parameter[Double] = currentPositionKey.set(0.0)

  // Retry count
  val retryCountKey: Key[Int]    = KeyType.IntKey.make("retryCount")
  var retryCount: Parameter[Int] = retryCountKey.set(0)

  // Event parameters
  val stageKey: Key[String]   = KeyType.StringKey.make("stage")
  val statusKey: Key[String]  = KeyType.StringKey.make("status")
  val messageKey: Key[String] = KeyType.StringKey.make("message")

  // Observation ID
  val obsId: ObsId = ObsId("2023A-001-456")
}
// Define the command key for the PACT HCD
