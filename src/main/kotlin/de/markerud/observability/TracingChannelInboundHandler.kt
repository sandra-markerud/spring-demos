package de.markerud.observability

import io.micrometer.context.ContextSnapshotFactory
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext

class TracingChannelInboundHandler(
    private val contextSnapshotFactory: ContextSnapshotFactory
) : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        contextSnapshotFactory.setThreadLocalsFrom<String>(ctx.channel()).use {
            ctx.fireChannelRead(msg)
        }
    }

}
