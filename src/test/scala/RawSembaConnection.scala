import java.util.logging.Logger

import app.testing.{AbstractSembaConnection, SembaConnectionImpl}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import sembaGRPC.SembaAPIGrpc.{SembaAPIBlockingStub, SembaAPIStub}
import sembaGRPC.{SembaAPIGrpc, UpdateMessage}

/**
  * Created by Eike on 01.05.2017.
  */

object RawSembaConnection {
  def apply(): RawSembaConnection = apply("localhost", 50051)
  def apply(host: String, port: Int): RawSembaConnection = {
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
    new RawSembaConnection(channel2, blockingStub, asyncStub)

  }
}

class RawSembaConnection(
  channel: ManagedChannel,
  blockingStub: SembaAPIBlockingStub,
  asyncStub: SembaAPIStub
  ) extends AbstractSembaConnection(channel, blockingStub, asyncStub) {
    override val logger = Logger.getLogger(classOf[RawSembaConnection].getName)

    override val updateFunction: PartialFunction[UpdateMessage, Any] = Map.empty

}

