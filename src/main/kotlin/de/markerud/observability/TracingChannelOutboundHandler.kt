package de.markerud.observability

import io.micrometer.context.ContextSnapshotFactory
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise

class TracingChannelOutboundHandler(
    private val contextSnapshotFactory: ContextSnapshotFactory
) : ChannelDuplexHandler() {

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        contextSnapshotFactory.setThreadLocalsFrom<String>(ctx.channel()).use {
            ctx.write(msg, promise)
        }
    }

    override fun flush(ctx: ChannelHandlerContext) {
        contextSnapshotFactory.setThreadLocalsFrom<String>(ctx.channel()).use { ctx.flush() }
    }

}
