package edu.alibaba;


import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword.SingleCpKsPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.kspir.SingleKsPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.kwpir.StdKwPirMain;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )throws Exception
    {
        System.out.println( "Hello World!" );
//        Logger logger = Logger.getLogger("com.xiya.test.LogDemo");
//        logger.setLevel(Level.ALL);
//
//        ConsoleHandler consoleHandler = new ConsoleHandler();
//        consoleHandler.setLevel(Level.FINEST);
//        logger.addHandler(consoleHandler);
//
//        logger.severe("严重");
//        logger.warning("警告");
//        logger.info("信息");
//        logger.config("配置");
//        logger.fine("良好");
//        logger.finer("较好");
//        logger.finest("最好");

//        int l = 32;
//        int byteL = CommonUtils.getByteLength(l);
//        System.out.println("byteL: " + byteL);
//        byte[] bot = new byte[byteL];
//        Arrays.fill(bot, (byte) 0xFF);
//        // print bot
//        System.out.println("bot: " + Arrays.toString(bot));
//        BytesUtils.reduceByteArray(bot, 19);
//        // print bot
//        System.out.println("bot: " + Arrays.toString(bot));

//        PtoDescManager.PrintPtoDesc();
//        System.out.println("--------------------------------------------------");
//        System.out.println(LJTPtoDesc.getInstance().getPtoName());
//        PtoDescManager.PrintPtoDesc();
//        System.out.println("--------------------------------------------------");
//        System.out.println(LJT2PtoDesc.getInstance().getPtoName());
//        PtoDescManager.PrintPtoDesc();
//        PtoDescManager.printAllPtoDesc();


        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);

//        System.out.println(File.separator);
//        Rpc ownRpc = RpcPropertiesUtils.readNettyRpcWithOwnName(properties, ownName, "server", "client");

        switch (ptoType) {
            case CpIdxPirMain.PTO_TYPE_NAME:
                CpIdxPirMain cpIdxPirMain = new CpIdxPirMain(properties, ownName);
                cpIdxPirMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
