import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Param;
import org.asynchttpclient.netty.NettyResponse;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class httpTest {

    public static NettyResponse getConnection(List<Param> params, String method, String apiurl, String x_rapidapi_host) {
        // 建立连接
        AsyncHttpClient client = new DefaultAsyncHttpClient();
        CompletableFuture completableFuture = client.prepare(method, apiurl)
                .addQueryParams(params)
                .setHeader("x-rapidapi-key", "8bf2a7f2f9msh678f4857e76bd45p16f005jsn25b2a7721455")
                .setHeader("x-rapidapi-host", x_rapidapi_host)
                .execute()
                .toCompletableFuture();
        NettyResponse nettyResponse = null;
        try {
            nettyResponse = (NettyResponse) completableFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nettyResponse;
    }

    public static Map<String, Object> finditemByJson(String jsonStr) {
        System.out.println("=================");
        System.out.println("当前json：" + jsonStr);
        JSONObject firstItemJson = JSONObject.parseObject(jsonStr);
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        Set set = firstItemJson.keySet();
        for (Object key : set) {
            Object value = firstItemJson.get(key);
            jsonMap.put((String) key, value);
            System.out.println(key + ":" + value);
        }
        return jsonMap;
    }

    public static List<Map<String, Object>> youtubeSearch(String query) {
        List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
        List<Param> params = new ArrayList<>();
        params.add(new Param("q", query));
        String method = "GET";
        String apiurl = "https://youtube-search-results.p.rapidapi.com/youtube-search/";
        String x_rapidapi_host = "youtube-search-results.p.rapidapi.com";
        NettyResponse nettyResponse = getConnection(params, method, apiurl, x_rapidapi_host);

        if (nettyResponse.getStatusCode() != 200) return itemsList;
        String body = nettyResponse.getResponseBody();
        JSONObject jsonres = JSONObject.parseObject(body);
        JSONArray jsonObject = jsonres.getJSONArray("items");
        for (Object o : jsonObject) {
            itemsList.add(finditemByJson(o.toString()));
        }
        return itemsList;

    }

    public static Map<String, Object> ytToMp3(String video_id) {

        Map<String, Object> resMap = new HashMap<String, Object>();
        List<Param> params = new ArrayList<>();
        params.add(new Param("video_id", video_id));
        String method = "GET";
        String apiurl = "https://youtube-to-mp32.p.rapidapi.com/yt_to_mp3";
        String x_rapidapi_host = "youtube-to-mp32.p.rapidapi.com";
        NettyResponse nettyResponse = getConnection(params, method, apiurl, x_rapidapi_host);

        if (nettyResponse.getStatusCode() != 200) return resMap;
        String body = nettyResponse.getResponseBody();
        resMap = finditemByJson(body);

        return resMap;

    }

    public static Map<String, Object> youtubeVideoInfo1(String url) {

        Map<String, Object> resMap = new HashMap<String, Object>();
        List<Param> params = new ArrayList<>();
        params.add(new Param("url", url));
        String method = "GET";
        String apiurl = "https://youtube-video-info1.p.rapidapi.com/youtube-info/";
        String x_rapidapi_host = "youtube-video-info1.p.rapidapi.com";
        NettyResponse nettyResponse = getConnection(params, method, apiurl, x_rapidapi_host);

        if (nettyResponse.getStatusCode() != 200) return resMap;
        String body = nettyResponse.getResponseBody();
        Map<String, Object> tmp = finditemByJson(body);
        resMap = finditemByJson(tmp.get("info").toString());

        return resMap;
    }

    public static Map<String, Object> fetchVideos(String videoId) throws IOException, ExecutionException, InterruptedException {

        Map<String, Object> resMap = new HashMap<String, Object>();
        List<Param> params = new ArrayList<>();
        params.add(new Param("videoId", videoId));
        String method = "GET";
        String apiurl = "https://youtube-videos.p.rapidapi.com/mp4";
        String x_rapidapi_host = "youtube-videos.p.rapidapi.com";
        NettyResponse nettyResponse = getConnection(params, method, apiurl, x_rapidapi_host);

        if (nettyResponse.getStatusCode() != 200) return resMap;
        String body = nettyResponse.getResponseBody();
        JSONObject jsonres = JSONObject.parseObject(body);
        JSONArray jsonObject = jsonres.getJSONArray("items");
        for (Object o : jsonObject) {
            resMap = finditemByJson(o.toString());
            break;
        }
        return resMap;
    }

    public static String youtubeScreenshot(String youtube_id, int seconds) throws IOException {

        Map<String, Object> resMap = new HashMap<String, Object>();
        List<Param> params = new ArrayList<>();
        params.add(new Param("youtube_id", youtube_id));
        params.add(new Param("seconds", String.valueOf(seconds)));
        String method = "GET";
        String apiurl = "https://youtube-screenshot1.p.rapidapi.com/frames/";
        String x_rapidapi_host = "youtube-screenshot1.p.rapidapi.com";
        NettyResponse nettyResponse = getConnection(params, method, apiurl, x_rapidapi_host);
        if (nettyResponse.getStatusCode() != 200) return "";

        InputStream inputStream = nettyResponse.getResponseBodyAsStream();    // 获取输入流
        //获取图片的二进制数据
        readInputStream(inputStream, youtube_id + ".jpg");
        System.out.println("finish!");
        return youtube_id + ".jpg";
    }

    public static void readInputStream(InputStream inputStream, String filename) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();//构造一个ByteArrayOutputStream
        byte[] buffer = new byte[1024];//设置一个缓冲区
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {//判断输入流长度是否等于-1  ，即非空
            outStream.write(buffer, 0, len);//把缓冲区的内容写入到输出流中，从0开始读取，长度为len
        }

        File imageFile = new File(filename);    //构造一个文件，保存图片到项目的根目录下
        FileOutputStream fileOutputStream = new FileOutputStream(imageFile);    //构造一个文件输出流FileOutputStream
        fileOutputStream.write(outStream.toByteArray());    //把文件数据写到输出流中
        fileOutputStream.close();
        inputStream.close();
        outStream.close();

        System.out.println("截图保存完成");
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
//        List<Map<String, Object>> youtube_search = youtubeSearch("Taylor+Swift");
//        for (int i = 0; i < 1; i++) {
//            for (Object key : youtube_search.get(i).keySet()) {
//                String value = youtube_search.get(i).get(key).toString();
//                System.out.println("key:"+key);
//                System.out.println("value"+value);
//                if (key.equals("id")) {
//                    Map<String, Object> fetchVideosMap = fetchVideos(value);
//                    ytToMp3(value);
//                    youtubeVideoInfo1(value);
//                    youtubeScreenshot(value, 3);
//                }
//            }
//        }
        Map<String, Object> res = fetchVideos("RsEZmictANA");
    }
}
