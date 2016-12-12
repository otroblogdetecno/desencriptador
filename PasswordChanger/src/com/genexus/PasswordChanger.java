package com.genexus;
import java.io.*;
import com.genexus.util.IniFile;
public class PasswordChanger
{
    static String passwordKey = "USER_PASSWORD";
    static String userKey = "USER_ID";

    static String NAMESPACE = "-namespace:";
    static String DATASTORE = "-datastore:";
    static String FILE         = "-file:";
    static String PASSWORD     = "-password:";
    static String USER        = "-user:";
    static String HELP1    = "-?";
    static String HELP2    = "-h";

    public static void main(String arg[])
    {
        String file = "client.cfg";
        String namespace = "default";
        String datastore = "DEFAULT";
        String user = null;
        String password = null;
//for para recorrer los parámetros
        for (int i = 0; i < arg.length; i++)
        {
            if        (arg[i].toLowerCase().startsWith(NAMESPACE))
            {
                System.err.println("arg[i].toLowerCase().startsWith(NAMESPACE)");
                namespace = arg[i].substring(NAMESPACE.length());
                System.err.println("namespace por parametros--->>>"+namespace);
            }
            else if (arg[i].toLowerCase().startsWith(DATASTORE))
            {
                System.err.println("arg[i].toLowerCase().startsWith(DATASTORE)");                
                datastore = arg[i].substring(DATASTORE.length());
            }
            else if (arg[i].toLowerCase().startsWith(PASSWORD))
            {
                System.err.println("arg[i].toLowerCase().startsWith(PASSWORD)");                
                password = arg[i].substring(PASSWORD.length());
            }
            else if (arg[i].toLowerCase().startsWith(USER))
            {
                System.err.println("arg[i].toLowerCase().startsWith(USER)");                
                user = arg[i].substring(USER.length());
            }
            else if (arg[i].toLowerCase().startsWith(FILE))
            {
                System.err.println("arg[i].toLowerCase().startsWith(FILE)");                                
                file = arg[i].substring(FILE.length());
            }
            else if (arg[i].toLowerCase().startsWith(HELP1))
            {
                System.err.println("arg[i].toLowerCase().startsWith(HELP1)");                                                
                usage();
            }
            else if (arg[i].toLowerCase().startsWith(HELP2))
            {
                usage();
            }
        }
//fin del for para recorrer los parámetros

        if    (password == null && password == null)
        {
            System.err.println("You must specify a user name or a password");
            usage();
        }

        if    (!new File(file).exists())
        {
            System.err.println("Can't open " + file);
            System.exit(1);
        }

        IniFile ini = new IniFile(file);
//---------trata de verificar un archivo con las claves crypto.cfg
        try
        {
            ini.setEncryptionStream(new FileInputStream("crypto.cfg"));
        }
        catch (java.io.IOException e)
        {
            System.out.println("Using default encryption keys...");
        }

        ConfigFileFinder.getConfigFile(null, file, null);
        //concadena namespace|datastore
        //busca el parametro cuando no encuentra pone el valor null
        //sirve solamente a efectos de búsqueda de un archivo válido
        if    (ini.getProperty(namespace + "|" + datastore, passwordKey) == null)
        {
            System.err.println("Invalid .cfg file format: can't find namespace/datastore");
            System.exit(1);
        }
        System.out.println("--->>> passwordKey="+PasswordChanger.passwordKey);
        System.out.println("--->>> namespace + | + datastore, passwordKey--->>>"+namespace+ "|" + datastore);
        System.out.println("--->>> ini.getProperty(namespace + | + datastore, passwordKey) == null"+ini.getProperty(namespace + "|" + datastore, passwordKey) == null);
        System.out.println("--->>> ini.getProperty(namespace + | + datastore, passwordKey)"+ini.getProperty(namespace + "|" + datastore, passwordKey));        
//se modifican las propiedades del archivo y se escriben las claves en el parámetro
//userKey
        if    (user != null){
            System.out.println("Ecribe si user != null -->>"+userKey+"  "+user);        
        /*escribe en el parametro USER_ID con usuario wspcs con la clave que es 
         * el namespace sumado a la barra y el dataspace
         * en el archivo original 
         * NAME_SPACE= default
         * NameSpace1= default
         */         
            ini.setPropertyEncrypted(namespace + "|" + datastore, userKey, user);
//            ini.setPropertyEncrypted("DEFAULT", userKey, user);            
            
        }
            
        if    (password != null){
            System.out.println("Ecribe si password != null -->>"+password);            
            System.out.println("Utiliza passwordKey -->>"+passwordKey);                        
        /*busca el parametro USER_ID utiliza la encriptacion del password
         * usando defaul|DEFAULT
         */
//            datastore="nada";
//            namespace="todo";
//        System.out.println("--->>> ANTES namespace + | + datastore, passwordKey--->>>"+namespace+ "|" + datastore);
            ini.setPropertyEncrypted(namespace + "|" + datastore, passwordKey, password);
        }
    
        System.out.println("GUARDA ARCHIVO -->> ");            
        ini.save();
    }

    private static void usage()
    {
        System.out.println("\ncom.genexus.PasswordChanger");
        System.out.println("parameters: -file:<filename>");
        System.out.println("            -namespace:<namespace>");
        System.out.println("            -datastore:<datastore>");
        System.out.println("            -user:<user>");
        System.out.println("            -password:<password>");
        System.exit(1);
    }
}