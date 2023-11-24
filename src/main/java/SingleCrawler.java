import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class SingleCrawler {

    public static void main(String[] args) throws Exception {

        ArrayList<Map<String,String>> arrayMap = new ArrayList<>();
        String iUrl = "https://rapidapi.com/theoddsapi/api/live-sports-odds/";
        String iCode = getCodeByPath(iUrl);

        Map<String,String> itemData = getDataFromCode(iCode); //获取当前api的信息
        itemData.put("category","Sports");

        String playUrl = itemData.get("playgroud");          //获取当前api的的参数页面url
        Map<String,String> parameterData = getDataFromPlaygroud(playUrl);   //根据参数页面的url获取api的参数信息

        Map<String,String> apiDataAll = new HashMap<>(); //将这两部分信息合并
        apiDataAll.putAll(itemData);
        apiDataAll.putAll(parameterData);
        apiDataAll.remove("playgroud");
        arrayMap.add(apiDataAll);

        String filename = "out/1.xls";
        export2(filename,apiDataAll);
        System.out.println("成功保存："+filename);

    }

    //根据URL获取源代码
    public static String getCodeByPath(String url) throws IOException {
        Connection conn = Jsoup.connect(url).timeout(5000);
        // 给连接添加模拟浏览器的header
        conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.header("Accept-Encoding", "gzip, deflate, sdch");
        conn.header("Accept-Language", "zh-CN,zh;q=0.8");
        conn.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");

        URL testUrl = new URL(url);
        URLConnection testCon = testUrl.openConnection();
        testCon.setRequestProperty("User-agent","Mozilla/4.0"); //服务器的安全设置不接受Java程序作为客户端访问: 在http连接时加入一行代码
        HttpURLConnection conForStateCode = (HttpURLConnection)testCon;
        int stateCode = conForStateCode.getResponseCode();
        if(stateCode != 200){
            return null;
        }
        String code = conn.execute().body();
        return code;
    }

    //根据源代码获取所有数据
    public static Map<String,String> getDataFromCode(String code){
        Map<String,String> apiData = new HashMap<String, String>();
        Document doc = Jsoup.parse(code);  //将源代码对象转换为document对象
        Elements elementsName = doc.getElementsByAttributeValue("class","center-content");  //api名称
        String[] nameAndContent = new String[2];
        for (Element element :elementsName) {
            Elements apiname = element.getElementsByTag("header"); //获取div
            nameAndContent = apiname.text().split(" API Documentation ");
            break;
        }

        String name = nameAndContent[0];
        System.out.println("API 名称: "+ name);
        apiData.put("name", name);

        Elements elementsPrice = doc.getElementsByClass("Name");  //api名称
        String str = elementsPrice.text();
        if (str.contains("FREEMIUM")) apiData.put("price", "FREEMIUM");
        else if(str.contains("FREE")) apiData.put("price", "FREE");
        else if(str.contains("PAID")) apiData.put("price", "PAID");
        else apiData.put("price", "Other");

        String content = nameAndContent[1];
        System.out.println("API 描述信息: "+ content);
        if(name.length() <= 2 || content == null) return null;
        apiData.put("content", content);

        String playgroud = doc.select("meta[name=og:url]").get(0).attr("content").replace("/api/","/playground/").replace("/zh/","/");
        System.out.println("playgroud:"+playgroud);
        apiData.put("playgroud", playgroud);

        return apiData;
    }

    //根据playgroud获取所有参数数据
    public static Map<String,String> getDataFromPlaygroud(String playUrl) throws IOException {

        Map<String,String> patameterData = new HashMap<String, String>();
        String playHtml = cutPlayHtml(playUrl);
        System.out.println(playHtml);
//        playHtml = deleteUnusefulStr(playHtml);

        if(playHtml == null || playHtml.length() <= 15){
            System.out.println("无法获取到playHtml");
            return patameterData;
        }

        JSONObject object = JSONObject.parseObject(playHtml);
        System.out.println(object);

        String method = object.getString("method");
        patameterData.put("method", method);

        String methodname = object.getString("name");
        patameterData.put("methodname", methodname);

        String apiroute = object.getString("route");
        String[] urlList = playUrl.split("/");
        String urlLastName = urlList[urlList.length-1];
        System.out.println("route的完整url为:"+"https://"+urlLastName+".p.rapidapi.com"+apiroute);
        patameterData.put("apiroute", "https://"+urlLastName+".p.rapidapi.com"+apiroute);

        String apidescription = object.getString("description");
        patameterData.put("apidescription", apidescription);

        String apiparams = object.getString("params");
//        if(apiparams == null) patameterData.put("apiparams", apiparams);
        patameterData.put("apiparams", apiparams);

        return patameterData;
    }

    // 根据url读取字节流
    public static String cutPlayHtml(String playUrl) throws IOException {
        URL url = new URL(playUrl);
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-agent","Mozilla/4.0"); //服务器的安全设置不接受Java程序作为客户端访问: 在http连接时加入一行代码
        HttpURLConnection conForStateCode = (HttpURLConnection)con;
        int stateCode = conForStateCode.getResponseCode();
        if(stateCode != 200){
            System.out.println("playgroud无法响应");
            return null;
        }

        InputStream inputStream = con.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder playHtml = new StringBuilder("playgroud");  // StringBuffer速度慢，但（线程）安全性高，StringBuilder速度快，但（线程）安全性差。
        String line;
        while ((line = reader.readLine()) != null) {
            playHtml.append(line);
        }
        reader.close();

        int beginIndex = playHtml.indexOf("{\"apiversion\":"); // 查找第一次出现的位置
        int endIndex = playHtml.substring(beginIndex+14).indexOf("{\"apiversion\":"); // 查找第二次出现的位置
        beginIndex = endIndex;
        endIndex = -1;
        String firstStr = "";
//        String firstStr = playHtml.substring(beginIndex,beginIndex+endIndex+13);  //截取第一次出现到第二次出现之前的字符串
        if(endIndex == -1) {
            endIndex = playHtml.substring(beginIndex + 14).indexOf(",\"parameters\":[{"); // 查找第二次出现的位置
            firstStr = playHtml.substring(beginIndex, beginIndex + endIndex + 14) + "}}";  //截取第一次出现到第二次出现之前的字符串
        }
        else if(endIndex == -1) {
            endIndex = playHtml.substring(beginIndex + 14).indexOf(",\"params\":null}"); // 查找第二次出现的位置
            firstStr = playHtml.substring(beginIndex, beginIndex + endIndex + 14) + ",\"params\":null}";  //截取第一次出现到第二次出现之前的字符串
        }
        System.out.println("endIndex:"+endIndex);
        System.out.println(firstStr);
        return firstStr;
    }

    public static void export2(String filename, Map<String,String> data){
        FileInputStream fs;
        try {
            fs = new FileInputStream(filename);
            POIFSFileSystem ps=new POIFSFileSystem(fs); //使用POI提供的方法得到excel的信息
            HSSFWorkbook wb=new HSSFWorkbook(ps);
            HSSFSheet sheet=wb.getSheetAt(0); //获取到工作表，因为一个excel可能有多个工作表
            HSSFRow row=sheet.getRow(0);
            int hang=0;
            if("".equals(row)||row==null){
                hang=0;
            }else{
                hang=sheet.getLastRowNum();
                hang=hang+1;
            }
            //分别得到最后一行的行号，和一条记录的最后一个单元格
            FileOutputStream out=new FileOutputStream(filename); //向d://test.xls中写数据
            row=sheet.createRow((short)(hang)); //在现有行号后追加数据
            String[] title = data.keySet().toArray(new String[0]);
            for (int i = 0; i < title.length; i++) {
                row.createCell(i).setCellValue(data.get(title[i])); //设置第一个（从0开始）单元格的数据
//                cell2.setCellValue(api.get(title[j]));
            }
//            row.createCell(0).setCellValue("安徽"); //设置第一个（从0开始）单元格的数据
//            row.createCell(1).setCellValue("安庆"); //设置第二个（从0开始）单元格的数据
            out.flush();
            wb.write(out);
            out.close();
            System.out.println(row.getPhysicalNumberOfCells()+" "+row.getLastCellNum());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //获取d://test.xls
    }

    public static void exportExcel(String filename, ArrayList<Map<String,String>> data) throws IOException {
        String[] title = data.get(0).keySet().toArray(new String[0]);
        System.out.println("title: "+title);
        // 创建一个工作簿
        HSSFWorkbook workbook = new HSSFWorkbook();
        // 创建一个工作表sheet
        HSSFSheet sheet = workbook.createSheet();
        // 创建第一行
        HSSFRow row = sheet.createRow(0);
        // 创建一个单元格
        HSSFCell cell = null;
        // 创建表头
        for (int i = 0; i < title.length; i++) {
            cell = row.createCell(i);
            // 设置样式
            HSSFCellStyle cellStyle = workbook.createCellStyle();
            // 设置字体
            HSSFFont font = workbook.createFont();
            font.setFontName("宋体");
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(title[i]);
        }

        // 从第二行开始追加数据
        for (int i = 1; i < (data.size() + 1); i++) {
            // 创建第i行
            HSSFRow nextRow = sheet.createRow(i);
            for (int j = 0; j < title.length; j++) {
                Map<String,String> api = data.get(i-1);
                HSSFCell cell2 = nextRow.createCell(j);
                cell2.setCellValue(api.get(title[j]));
            }
        }

        // 创建一个文件
        File file = new File(filename);
        try {
            file.createNewFile();
            // 打开文件流
            FileOutputStream outputStream = FileUtils.openOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("============导出成功！============");
    }

    public static void test(String str) throws IOException {
        URL url = new URL(str);
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-agent","Mozilla/4.0");
        InputStream inputStream = con.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        BufferedWriter writer = new BufferedWriter(new FileWriter("1.html"));
        String line;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            writer.write(line);
            writer.newLine();
        }

        reader.close();
        writer.close();
    }

}