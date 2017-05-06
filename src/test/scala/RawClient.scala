import java.util.logging.Logger

import app.testing.{AbstractSembaConnection, SembaConnectionImpl$}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import sembaGRPC.SembaAPIGrpc.{SembaAPIBlockingStub, SembaAPIStub}
import sembaGRPC.{SembaAPIGrpc, UpdateMessage}

/**
  * Created by Eike on 01.05.2017.
  */

object RawClient {
  def apply(): RawClient = apply("localhost", 50051)
  def apply(host: String, port: Int): RawClient = {
    /* val test = ManagedChannelBuilder.forAddress(host, port)Ja
     test.usePlaintext(true)
     val  channel =  test.build()
      */



    val channel = ManagedChannelBuilder.forAddress(host, port).asInstanceOf[ManagedChannelBuilder[_]]
    channel.maxInboundMessageSize(1012*1014*1024)
    channel.usePlaintext(true)
    val channel2 = channel.build()
    val blockingStub = SembaAPIGrpc.blockingStub(channel2)
    val asyncStub = SembaAPIGrpc.stub(channel2)
    new RawClient(channel2, blockingStub, asyncStub)

  }
}

class RawClient(
  channel: ManagedChannel,
  blockingStub: SembaAPIBlockingStub,
  asyncStub: SembaAPIStub
  ) extends AbstractSembaConnection(channel, blockingStub, asyncStub) {
    override val logger = Logger.getLogger(classOf[RawClient].getName)

    override val updateFunction: PartialFunction[UpdateMessage, Any] = Map.empty

}

