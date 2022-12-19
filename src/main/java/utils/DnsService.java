package utils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DnsService {
    private static final Properties env;

    static {
        env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        // https://download.oracle.com/otn_hosted_doc/jdeveloper/904preview/jdk14doc/docs/guide/jndi/jndi-dns.html
    }

    /**
     * Returns an array of CNAME strings given a host
     *
     * @param host Host Address
     * @return String[]: array of CNAMEs
     * @throws NamingException Throws exception if the host is invalid
     */
    public static String[] getCNAME(String host) throws NamingException {
        List<String> cnames = new ArrayList<>();
        InitialDirContext idc = new InitialDirContext(env);
        Attribute cnameAttrib = idc.getAttributes(host, new String[]{"CNAME"}).get("CNAME");

        if (cnameAttrib == null) {
            return new String[]{host};
        }

        NamingEnumeration<?> cnameEnum = cnameAttrib.getAll();
        while (cnameEnum.hasMoreElements()) {
            cnames.add(cnameEnum.next().toString());
        }

        return cnames.toArray(new String[0]);
    }
}