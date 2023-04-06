/**
 * PROJECT   : Elbfisch - java process automation controller (jPac) 
 * MODULE    : Modbus server CommandHandler.java (versatile input output subsystem)
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 *
 * This file is part of the jPac process automation controller.
 * jPac is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jPac is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpac.vioss.modbus.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jpac.vioss.ef.SignalTransport;
import org.jpac.vioss.modbus.FunctionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author berndschuster
 */
public class CommandHandler extends ChannelInboundHandlerAdapter {
    protected Logger    Log       = LoggerFactory.getLogger("jpac.vioss.modbus.server");
    
    static final List<CommandHandler> listOfActiveCommandHandlers = new ArrayList<>();

    protected ByteBuf in;
    protected ByteBuf out;

    protected boolean                           firstSignalValueTransmission;
    protected InetSocketAddress                 remoteSocketAddress;
    protected MessageFactory                    messageFactory;
    protected SignalTransport                   target;
    protected int                               index;     
    protected Mbap                              mbap;
    protected HashMap<FunctionCode, Message>    recycledMessages;//used as a buffer for recycling messages
    
    public CommandHandler(InetSocketAddress remoteSocketAddress, DataBlocks datablocks){
        this.remoteSocketAddress                 = remoteSocketAddress;
        this.firstSignalValueTransmission        = true;
        
        this.recycledMessages                    = new HashMap<>();
        this.messageFactory                      = new MessageFactory(recycledMessages, datablocks);
        this.mbap                                = new Mbap();
        
        synchronized(listOfActiveCommandHandlers) {
        	listOfActiveCommandHandlers.add(this);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        out = ctx.alloc().buffer(32000);
    }
     
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Command         command = null;
        Acknowledgement acknowledgement;
        try{        
            in      = (ByteBuf) msg;
            //read out modbus application protocol
            mbap.decode(in);
            //read modbus command
            try{
                command = (Command)messageFactory.getRecycledMessage(in);
                Log.debug("received command {}", command);
                acknowledgement = command.handleRequest(this);
            } catch(InvalidFunctionCodeException exc){
                acknowledgement = new InvalidFunctionCodeAcknowledgement(messageFactory.getFctCode());
            }
        } catch(Exception exc){
            acknowledgement = new FunctionFailedAcknowledgement(messageFactory.getFctCode());
            Log.error("Error: ", exc);
        }
        out.clear();
        Log.debug("writing received mbap {}", mbap);
        mbap.encode(out, acknowledgement);
        Log.debug("{} acknowledged with {}", command, acknowledgement);
        acknowledgement.encode(out);
        out.retain();//"out" should be reused for all acknowledgements until context is closed
        ctx.writeAndFlush(out);
        in.release();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        super.channelInactive(ctx);
        synchronized (listOfActiveCommandHandlers) {			
            if (listOfActiveCommandHandlers.contains(this)){
                listOfActiveCommandHandlers.remove(this);
            }
        }
        Log.info("remote connection for modbus.server://" + remoteSocketAddress.getHostName() + ":" + remoteSocketAddress.getPort() + " closed");
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        super.channelActive(ctx);
        Log.info("remote connection for modbus.server://" + remoteSocketAddress.getHostName() + ":" + remoteSocketAddress.getPort() + " established");
        firstSignalValueTransmission = true;//invoke transfer of all client input signals on first transmission regardless if changed or not
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception{
        super.channelActive(ctx);
        synchronized (listOfActiveCommandHandlers) {			
            if (listOfActiveCommandHandlers.contains(this)){
                listOfActiveCommandHandlers.remove(this);
            }
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
        
    public static List<CommandHandler> getListOfActiveCommandHandlers(){
        return listOfActiveCommandHandlers;
    }
}
