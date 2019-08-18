package com.clj.blesample;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface IService {
    //天气网址：https://www.weather.com.cn/data/sk/101010100.html
    //@GET("data/sk/{location}")
    //Call<WeatherBean> weatherInfo(@Path("location") String locationCode);
    @GET("now.json")
    Call<ResponseBody> weatherInfo(@Query("key")String key, @Query("location")String location);

//    //获取天气数据，baseURL在model类里声明
//    @GET("now")
//    Observable<Weather> getNowWeather(@Query("location")String location,@Query("key")String key);

}
