package com.genexus;
import java.io.*;
import com.genexus.util.IniFile;
public class Desencriptador
{
    static String passwordKey = "USER_PASSWORD";
    static String userKey = "USER_ID";

    public static void main(String arg[])
    {
        String file = "client.cfg";
        String namespace = "default";
        String datastore = "DEFAULT";
        String user = null;
        String password = null;


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
        System.out.println("--->>> Sección del archivo client.cfg donde se busca usuario y contraseña -->>>"+namespace+ "|" + datastore);
        System.out.println("--->>> USUARIO                 --->>>"+ini.getPropertyEncrypted(namespace + "|" + datastore, userKey));        
        System.out.println("--->>> USUARIO ENCRIPTADO      --->>>"+ini.getProperty(namespace + "|" + datastore, userKey));                
        System.out.println("--->>> CONTRASEÑA              --->>>"+ini.getPropertyEncrypted(namespace + "|" + datastore, passwordKey));        
        System.out.println("--->>> CONTRASEÑA ENCRIPTADA   --->>>"+ini.getProperty(namespace + "|" + datastore, passwordKey));
    }

    private static void usage()
    {
        System.out.println("=====================================");
        System.out.println("DESENCRIPTADOR DE ARCHIVOS CLIENT.CFG");
        System.out.println("=====================================");
        System.out.println("Toma un archivo client.cfg y desencripta");
        System.out.println("el usuario y contraseña generado por la");
        System.out.println("utilidad PasswordChanger de Genexus");
        System.out.println("utiliza la passwordKey y userKey original");
        System.out.println("del archivo PasswordChanger");
        System.exit(1);
    }//usage
}//clase