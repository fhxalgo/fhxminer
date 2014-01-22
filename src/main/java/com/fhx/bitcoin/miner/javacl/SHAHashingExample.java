package com.fhx.bitcoin.miner.javacl;

import java.security.MessageDigest;
import java.util.Arrays;

public class SHAHashingExample
{
    public static void main(String[] args)throws Exception
    {
        String password = "123456789";

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes());

        byte[] byteData = md.digest();

        System.out.println("password length: " + password.length());
        System.out.println("byte length    : " + byteData.length);

        System.out.println("byteData: " + Arrays.toString(byteData));

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        System.out.println("Hex format : " + sb.toString());

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<byteData.length;i++) {
            String hex=Integer.toHexString(0xff & byteData[i]);
            if(hex.length()==1) hexString.append('0');
            System.out.print(hex + ",");
            hexString.append(hex);
        }
        System.out.println();
        System.out.println("Hex format : " + hexString.toString());
    }
}