package se.scalablesolutions.akka.actor

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import se.scalablesolutions.akka.dispatch.Dispatchers
import org.scalatest.matchers.MustMatchers
import java.util.concurrent.CountDownLatch
import Actor._

/**
 * Tests the behaviour of the executor based event driven dispatcher when multiple actors are being dispatched on it.
 *
 * @author Jan Van Besien
 */
class ExecutorBasedEventDrivenDispatcherActorsSpec extends JUnitSuite with MustMatchers {
  class SlowActor(finishedCounter: CountDownLatch) extends Actor {
    self.dispatcher = Dispatchers.globalExecutorBasedEventDrivenDispatcher
    self.id = "SlowActor"

    def receive = {
      case x: Int => {
        Thread.sleep(50) // slow actor
        finishedCounter.countDown
      }
    }
  }

  class FastActor(finishedCounter: CountDownLatch) extends Actor {
    self.dispatcher = Dispatchers.globalExecutorBasedEventDrivenDispatcher
    self.id = "FastActor"

    def receive = {
      case x: Int => {
        finishedCounter.countDown
      }
    }
  }

  @Test def slowActorShouldntBlockFastActor {
    val sFinished = new CountDownLatch(50)
    val fFinished = new CountDownLatch(10)
    val s = newActor(() => new SlowActor(sFinished)).start
    val f = newActor(() => new FastActor(fFinished)).start
     
    // send a lot of stuff to s
    for (i <- 1 to 50) {
      s ! i
    }

    // send some messages to f
    for (i <- 1 to 10) {
      f ! i
    }

    // now assert that f is finished while s is still busy
    fFinished.await
    assert(sFinished.getCount > 0)
    f.stop
    s.stop
  }
}
