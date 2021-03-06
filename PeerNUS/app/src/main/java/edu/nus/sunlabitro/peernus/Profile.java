package edu.nus.sunlabitro.peernus;

import java.util.ArrayList;

public class Profile {
    private int id;
    private String name;
    private String nusnet;
    private int matricYear;
    private ArrayList<String> course;
    private int profilePic;

    public Profile() {

    }

    public Profile(int id, String name, String nusnet, int matricYear, ArrayList<String> course, int profilePic) {
        this.id = id;
        this.name = name;
        this.nusnet = nusnet;
        this.matricYear = matricYear;
        this.course = course;
        this.profilePic = profilePic;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNusnet() {
        return nusnet;
    }

    public int getMatricYear() {
        return matricYear;
    }

    public ArrayList<String> getCourse() {
        return course;
    }

    public int getProfilePic() {
        return profilePic;
    }
}
