package com.github.ong.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileCopy {

    static final int SIZE=1024*1024*15;

    public static void dirCopy(String src,String des)throws Exception{
        File srcFile=new File(src);

        File desFile=new File(des);
        if(!desFile.exists()){
            desFile.mkdir();
        }
        File []fs=null;
        //如果srcFiles是文件
        if(srcFile.isFile()){
            fs=new File[]{srcFile};
        }else if(srcFile.isDirectory()){
//            desFile=new File(desFile.getAbsolutePath()+"/"+srcFile.getName());
//            desFile.mkdir();
            fs=srcFile.listFiles();
        }

        for(File f:fs){
            String newSrc=f.getAbsolutePath();
            String newDes=desFile.getAbsolutePath()+"/"+f.getName();
            if(f.isFile()){
                FileCopy.flieCopy(newSrc, newDes);
            }else if(f.isDirectory()){
                dirCopy(newSrc, newDes);
            }
        }
    }

    public static void flieCopy(String src,String des)throws Exception{
        File srcFile=new File(src);
        File desFile=new File(des);
        FileInputStream fis=new FileInputStream(srcFile);
        FileOutputStream fos=new FileOutputStream(desFile);
        byte b[]=new byte[FileCopy.SIZE];
        int n;
        while((n=fis.read(b))!=-1){
            fos.write(b,0,n);
        }
        fos.close();
        fis.close();
    }

    public static void main(String[] args) throws Exception {
        dirCopy("D:\\IDEAProject\\mavenplugin\\src\\main\\resources","D:\\IDEAProject\\mavenplugin\\target");
    }

}
