package com.example.demo.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;


import java.util.Map;
import java.util.List;



@Service
public class GeminiService {


    @Value("${gemini.api.key}")
    private String apiKey;



    private final RestTemplate restTemplate =
            new RestTemplate();




    public String ask(String question){



    	String url =
    			"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
    			+ apiKey;




        Map<String,Object> body =
                Map.of(

                    "contents",
                    List.of(

                        Map.of(

                            "parts",
                            List.of(

                                Map.of(
                                    "text",
                                    "あなたは農業専門アシスタントです。"
                                    + "日本語で分かりやすく回答してください。\n\n"
                                    + question
                                )

                            )

                        )

                    )

                );




        try{


            Map response =
                    restTemplate.postForObject(
                            url,
                            body,
                            Map.class
                    );



            // Gemini回答取得

            List candidates =
                    (List) response.get("candidates");



            Map candidate =
                    (Map) candidates.get(0);



            Map content =
                    (Map) candidate.get("content");



            List parts =
                    (List) content.get("parts");



            Map part =
                    (Map) parts.get(0);



            return part.get("text").toString();




        }catch(Exception e){


            return "AI通信エラー："
                    + e.getMessage();


        }


    }


}