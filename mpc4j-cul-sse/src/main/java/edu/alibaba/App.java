package edu.alibaba;

import edu.alibaba.main.CulSseMain;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;

import java.io.IOException;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, MpcAbortException {
        System.out.println( "Hello World!" );
        System.out.println( "Hello World!" );
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);

        switch (ptoType) {
            case CpIdxPirMain.PTO_TYPE_NAME:
                CpIdxPirMain cpIdxPirMain = new CpIdxPirMain(properties, ownName);
                cpIdxPirMain.runNetty();
                break;
            case CulSseMain.PTO_TYPE_NAME:
                CulSseMain culSseMain = new CulSseMain(properties, ownName);
                culSseMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
