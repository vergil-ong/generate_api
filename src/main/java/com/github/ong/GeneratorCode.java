package com.github.ong;

import com.github.ong.api.api.annotation.API;
import com.github.ong.api.api.annotation.support.ParamType;
import com.github.ong.api.api.annotation.support.RequestMethod;
import com.github.ong.api.api.annotation.support.RequestParam;
import com.github.ong.template.GeneratorDocs;
import com.github.ong.util.FormatUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.*;

@Mojo(name = "GenerateAPI")
public class GeneratorCode extends AbstractMojo {

    @Parameter( property = "name", defaultValue = "001" )
    private String name;

    @Parameter(property = "project.build.sourceDirectory")
    private String sourceDirectory;

    @Parameter(property = "basedir")
    private String basedir;

    @Parameter(property = "project.artifactId")
    private String artifactId;

    private ClassLoader loader;

    private String classpath;

    private Map<String,Object> apiData;

    public static String trueChinese = "是";
    public static String falseChinese = "否";

    private String templateFile = "api_template.html";


    private String targetDir;

    @Parameter
    private String templateDir;

    private String libDir;

//    private String templateDir = "D:\\IDEAProject\\mavenplugin\\src\\main\\resources\\";
//    private String templateDir = GeneratorCode.class.getResource("/").getPath();

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public void setApiData(Map<String, Object> apiData) {
        this.apiData = apiData;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("============王硕插件生效了================");
        getLog().info("this name is "+name);
        getLog().info("this sourceDirectory is "+sourceDirectory);
        getLog().info("this basedir is "+basedir);
        getLog().info("this artifactId is "+artifactId);
        libDir = basedir+"\\target\\"+artifactId+"\\WEB-INF\\lib";
        getLog().info("this libDir is "+libDir);

        String classpath = basedir+"\\target\\classes\\";
        setClasspath(classpath);
        getLog().info("this classpath is "+classpath);

        targetDir = basedir + "\\target\\";
        setTargetDir(targetDir);

        getLog().info("template_dir is "+templateDir);

        //扫描所有Java
        ArrayList<File> files = new ArrayList<File>();
        scanFiles(basedir,files);

//        getLog().info("files");
//        for(File file : files){
//            getLog().info("file "+file.getPath());
//        }

        try {
            ClassLoader loader = buildCLassLoader();
            setLoader(loader);
            getLog().info("setLoader is OK");
            //匹配出 所有@api
            List<Class<?>> classList = scanAnnotations(files);
            getLog().info("scanAnnotations is OK");
            //生成API数据
            analysisClasses(classList);
            getLog().info("analysisClasses is OK");
            //根据API模板渲染数据
            render();
            getLog().info("render is OK");
            this.getLog().info("file path is "+targetDir+"\\api.html");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param fileDir
     * @param resultFiles
     */
    public void scanFiles(String fileDir, List<File> resultFiles){
        File currentFile = new File(fileDir);

        if(currentFile == null){
            return;
        }

        if(currentFile.isDirectory()){
            File[] listFiles = currentFile.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    if (pathname.isDirectory() || pathname.getName().endsWith(".java")|| pathname.getName().endsWith(".jar")) {
                        //从根目录下取Java文件或目录
                        return true;
                    }
                    return false;
                }
            });

            for(File file:listFiles){
                if(file.isFile()){
                    resultFiles.add(file);
                }else if(file.isDirectory()){
                    scanFiles(file.getPath(),resultFiles);
                }

            }
        }
    }

    public List<Class<?>> scanAnnotations(List<File> fileList) throws ClassNotFoundException {
        List<Class<?>> classList = new ArrayList<Class<?>>();
        if(fileList == null){
            return classList;
        }
        for(File file : fileList){
            String absolutePath = file.getPath().replace(sourceDirectory, "");
            String classPath = absolutePath.replace('\\', '.' );
            classPath = classPath.startsWith(".")?classPath.substring(1,classPath.length()):classPath;
            classPath = classPath.substring(0,classPath.length()-".java".length());
//            this.getLog().info("classPath : "+classPath);
            if(classPath.contains("Controller")||classPath.contains("controller")){
                Class<?> clazz = Class.forName(classPath,true,loader);
                classList.add(clazz);
            }
        }

        return classList;
    }

    public ClassLoader buildCLassLoader() throws IOException {

        URLStreamHandler sh = null;
        String basePath = (new URL("file",null,new File(classpath).getCanonicalPath())).toString();
        List<URL> classesUrl = new ArrayList<URL>();
        classesUrl.add(new URL(null,basePath+"\\",sh));
//        String libPath = "D:\\treatment\\target\\treatment\\WEB-INF\\lib";
        List<String> libList = parseLibsPathList(libDir);
        basePath = (new URL("file",null,new File(libDir).getCanonicalPath())).toString();
        for(String libFileName : libList){
            classesUrl.add(new URL(null,basePath+"\\"+libFileName,sh));
        }
        ClassLoader currentLoader = new URLClassLoader(classesUrl.toArray(new URL[classesUrl.size()]),Thread.currentThread().getContextClassLoader());
        return currentLoader;
    }

    public List<String> parseLibsPathList(String libPath){
        List<String> list = new ArrayList<String>();
        File libFile = new File(libPath);
        if(libFile.isDirectory()){
            File[] files = libFile.listFiles();
            for(File file : files){
                list.add(file.getName());
            }
        }
        return list;
    }

    public void analysisClasses(List<Class<?>> list){

        apiData = new HashMap<>();

        int i = 0;
        for (Class<?> clazz :list) {
            Method[] methods = clazz.getDeclaredMethods();
            analysisMethods(i,methods);
            i++;
        }
    }

    public void analysisMethods(int index, Method[] methods){
        int i =1;
        for(Method method:methods){
            boolean annotationPresent = method.isAnnotationPresent(API.class);
            if(!annotationPresent){
                continue;
            }
            API api = method.getAnnotation(API.class);
            System.out.println("api name is "+api.name());
            Map<String,Object> itemMap  = new HashMap<>();
            itemMap.put("desc",api.description());
            itemMap.put("method", parseRequestMethod(api.method()));
            itemMap.put("testURL",api.urlMapping());
            itemMap.put("paramArray",parseRequestParams(api.params()));
            itemMap.put("responseExample", FormatUtil.formatJson(api.examples()));

            apiData.put("C:"+index+" M:"+i+api.name(),itemMap);
            i++;
        }
    }

    public void render(){
        GeneratorDocs gd = new  GeneratorDocs();

        gd.setTargetDir(targetDir);
        gd.setTemplateSourceFile(templateFile);
        gd.loadTemplate(templateDir);

        Map<String,Object> params = new HashMap<>();
        params.put(GeneratorDocs.PAYLOADPARAM,apiData);
        Map<String, String> globalParams = gd.buildGlobalParams();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        globalParams.put("versiontime",sdf.format(new Date()));
        params.put(GeneratorDocs.GLOBALPARAM,globalParams);
        try {
            gd.render(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String parseRequestMethod(RequestMethod... requestMethods){
        StringBuilder stringBuilder = new StringBuilder();
        if(requestMethods == null){
            return stringBuilder.toString();
        }

        for(RequestMethod method: requestMethods){
            stringBuilder.append(',')
                        .append(method.name());
        }
        stringBuilder.deleteCharAt(0);

        return stringBuilder.toString();
    }

    public List<String[]> parseRequestParams(RequestParam... requestParams){
        ArrayList<String[]> stringList = new ArrayList<>();
        if(requestParams == null){
            return stringList;
        }

        for(RequestParam requestParam: requestParams){
            String[] strArray = new String[5];
            strArray[0] = requestParam.name();
            strArray[1] = parseBol2Chinese(requestParam.isRequired());
            strArray[2] = requestParam.type().getName();
            strArray[3] = requestParam.desc();
            strArray[4] = requestParam.defaultValue();
            stringList.add(strArray);
        }
        return stringList;
    }

    public static String parseBol2Chinese(boolean bol){
        if(bol){
            return trueChinese;
        }else{
            return falseChinese;
        }
    }

    public static void main(String[] args) throws Exception {
        GeneratorCode generatorCode = new GeneratorCode();
        String basedir = "D:\\treatment";
        String sourceDirectory = "D:\\treatment\\src\\main\\java";
        String classPath = "D:\\treatment\\target\\classes\\";
        generatorCode.setSourceDirectory(sourceDirectory);
        generatorCode.setClasspath(classPath);

        ArrayList<File> files = new ArrayList<File>();
        generatorCode.scanFiles(basedir,files);
        ClassLoader classLoader = generatorCode.buildCLassLoader();
        generatorCode.setLoader(classLoader);

        List<Class<?>> classes = generatorCode.scanAnnotations(files);

        generatorCode.analysisClasses(classes);


//        System.out.println(Class.forName("java.lang.String"));
//        System.out.println(Class.forName("com.github.ong.controller.IndexController"));

    }


}
