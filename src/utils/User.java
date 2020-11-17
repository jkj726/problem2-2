package utils;

import course.Bidding;
import course.Course;
import server.Server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class User {
    public String studentId;
    public List<Bidding> biddingList;
    public int numTotalMileage;
    public List<Course> registeredCourseList;

    public User(String Id, List<Bidding> List){
        studentId = Id;
        biddingList = List;
        Iterator<Bidding> iterator = biddingList.iterator();
        int sumMileage= 0;
        while (iterator.hasNext()){
            sumMileage += iterator.next().mileage;
        }
        numTotalMileage = sumMileage;
        registeredCourseList = new ArrayList<>();

    }
}
