package com.github.ong.template;

import com.alibaba.fastjson.JSONObject;
import com.github.ong.util.FileCopy;
import com.github.ong.util.FormatUtil;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.ResourceLoader;
import org.beetl.core.Template;
import org.beetl.core.resource.FileResourceLoader;
import org.beetl.core.resource.StringTemplateResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 文档生成器
 * @Author Wangshuo[wangshuo@lenovocloud.com]
 * @Date 2018/4/1 15:01
 */
public class GeneratorDocs {

    private String templateSourceFile;

    private ResourceLoader resourceLoader;

    private String targetDir;

    private String templateDir;

    public void setTemplateSourceFile(String templateSourceFile) {
        this.templateSourceFile = templateSourceFile;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    public static final String GLOBALPARAM = "globalParam";

    public static final String PAYLOADPARAM = "payloadParam";

    public GeneratorDocs(){
        setTemplateDir(templateDir);
        loadTemplate(templateDir);
    }

    /**
     * 加载模板
     * @return 是否成功
     */
    public boolean loadTemplate(String path){
        resourceLoader = new FileResourceLoader(path, "UTF-8");
        setTemplateDir(path);
        return true;
    }

    /**
     * 渲染
     * @return 渲染是否成功
     */
    public boolean render(Map<String, Object> params) throws Exception {
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        Template t = gt.getTemplate(templateSourceFile);
        Map<String, String> globalParams = (Map<String, String>) params.get(GLOBALPARAM);
        Map<String, Object> payloadParam = (Map<String, Object>) params.get(PAYLOADPARAM);
        t.binding("globalParams",globalParams);
        t.binding("params",payloadParam);

        String str = t.render();
        String filePath = targetDir+"\\api.html";
        FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
        t.renderTo(fileOutputStream);
        FileCopy.dirCopy(templateDir,targetDir);
        return true;
    }



    public Map<String,String> buildGlobalParams(){
        Map<String,String> map  = new LinkedHashMap<>();
        map.put("HTTPCODE:200","请求成功");
        map.put("HTTPCODE:404","服务端异常");
        map.put("HTTPCODE:401","会话异常或超时");
        map.put("性别","男:1;女:0");
        map.put("自我评价","非常轻松:1;刚刚好:2;非常吃力:3");
        map.put("食物归属","蛋白质:1;碳水化合物:2;脂肪:3");
        return map;
    }

    public Map<String,Object> buildParams(){

        String[] strArray = new String[4];
        strArray[0] = "param1";
        strArray[1] = "是";
        strArray[2] = "string";
        strArray[3] = "这是参数1";

        List<String[]> stringList = new ArrayList<>();
        stringList.add(strArray);

        strArray = new String[4];
        strArray[0] = "param2";
        strArray[1] = "否";
        strArray[2] = "double";
        strArray[3] = "这是参数2";
        stringList.add(strArray);

        strArray = new String[4];
        strArray[0] = "param3";
        strArray[1] = "否";
        strArray[2] = "Integer";
        strArray[3] = "这是参数3";
        stringList.add(strArray);


        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("title","title_value");
        resultMap.put("type","type_value");
        resultMap.put("create_time","create_time_value");
        resultMap.put("update_time","update_time_value");

        Map<String,Object> responseMap = new HashMap<>();
        responseMap.put("status","0");
        responseMap.put("requestid","1231231230");
        responseMap.put("msg","OK");
        responseMap.put("result",resultMap);

        String jsonString = JSONObject.toJSONString(responseMap);
        jsonString = FormatUtil.formatJson(jsonString);



        Map<String,Object> itemMap  = new HashMap<>();
        itemMap.put("desc","接口描述1");
        itemMap.put("method","GET,POST");
        itemMap.put("testURL","http://test.com/v2/testing");
        itemMap.put("paramArray",stringList);
        itemMap.put("responseExample",jsonString);

        Map<String,Object> map  = new HashMap<>();
        map.put("接口示例1",itemMap);
        return map;
    }

    public static void main(String[] args) throws Exception {
        GeneratorDocs gd = new  GeneratorDocs();
        String templatePath = "api_template.html";
        String targetDir = "D:\\IDEAProject\\mavenplugin\\target";
        gd.setTargetDir(targetDir);
        gd.setTemplateSourceFile(templatePath);
        gd.loadTemplate("D:\\IDEAProject\\mavenplugin\\src\\main\\resources");
//        gd.render();
    }
}
