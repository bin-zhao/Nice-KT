package com.lj.core.net.msg

import com.lj.core.common.CmdExecutor
import com.lj.core.common.HandlerAnnotation
import com.lj.core.net.Opcode
import com.lj.core.net.SocketManager
import com.lj.core.service.GameService
import com.lj.core.service.Msg
import com.lj.core.service.kotlin.dispatchAwait
import com.lj.core.utils.ClassScanner
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kt.crypto.decodeBase64
import kt.scaffold.net.DiscoveryManager
import kt.scaffold.tools.logger.Logger
import java.lang.Exception
import java.lang.RuntimeException

object MessageDispatcher {

    private val cmdHandlers = mutableMapOf<Short, CmdExecutor>()

    fun initialize(packageName:String){
        val messageCommandClasses = ClassScanner.listClassesWithAnnotation(packageName,HandlerAnnotation::class.java)
        for(cls in messageCommandClasses){
            try{
                val handler = cls.getDeclaredConstructor().newInstance()
                val method = cls.getMethod("process",String::class.java, Msg::class.java)
                val msgId = cls.getAnnotation(HandlerAnnotation::class.java).opcode

                var cmdExecutor = cmdHandlers[msgId]
                if (cmdExecutor!=null) throw RuntimeException("cmd[$msgId] duplicated")

                cmdExecutor = CmdExecutor(method, handler as Any)
                cmdHandlers[msgId] = cmdExecutor

            }catch (e:Exception){
                Logger.error("initialize error: ${e.cause}")
            }
        }
    }

    fun dispatch(socketId:String, msg: Msg){
        val cmdExecutor: CmdExecutor? = cmdHandlers[msg.msgId]
        if(cmdExecutor == null){
            Logger.error("message executor missed, cmd=${msg.msgId}")
            return
        }

        try {
            cmdExecutor.method.invoke(cmdExecutor.handler,socketId, msg)
        }catch (e:Exception){
            Logger.error("dispatch error: ${msg.msgId}, ${e.cause}")
        }
    }

    fun dispatchService(socketId: String, serverId:Int, serverType:Int, msg:Msg){
        GlobalScope.launch {
            var record = DiscoveryManager.getServerRecordAwait(serverId,serverType)
            if(record == null){
                Logger.debug("gate dispatch error..${msg.msgId},$serverId,$serverType")
                record = DiscoveryManager.chooseServerRecordAwait(serverType)
            }
            if(record!=null){
                msg.serverId = record.metadata.getInteger("server_id").toShort()
                msg.serverType = record.metadata.getInteger("server_type").toByte()

                val reference = DiscoveryManager.getReferenceAwait(record)
                val service = reference.getAs(GameService::class.java)
                val msgResp = service.dispatchAwait(msg)

                if(msgResp.msgId == Opcode.MSG_G2C_LoginGate && msgResp.userId >0 ){
                    //绑定用户ID 和Socket ID
                    SocketManager.bindUserid2Socket(msgResp.userId,socketId)
                }
                SocketManager.sendMsg(
                    socketId,
                    msgResp.seq,
                    msgResp.msgId,
                    msgResp.serverId,
                    msgResp.serverType,
                    msgResp.base64Msg.decodeBase64()
                )

            }
        }

    }
}