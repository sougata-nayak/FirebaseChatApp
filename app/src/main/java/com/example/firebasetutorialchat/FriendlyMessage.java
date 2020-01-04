package com.example.firebasetutorialchat;

public class FriendlyMessage {

    public String text;
    public String name;
    public String photoUrl;
    public String time;

    public FriendlyMessage(){}

    public FriendlyMessage(String text, String name, String photoUrl, String time) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
