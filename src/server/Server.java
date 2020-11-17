package server;

import course.*;
import utils.Config;
import utils.ErrorCode;
import utils.Pair;
import utils.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Server {

    public List<Course> search(Map<String,Object> searchConditions, String sortCriteria){
        // TODO Problem 2-1


        List<Course> allCourseList = readCourses("data/Courses/2020_Spring/");
        List<Course> searchResult = new ArrayList<>();


        if (searchConditions.isEmpty() || searchConditions == null) {
            if (sortCriteria == null){
                return allCourseList;
            } else {
                return sortByCriteria(allCourseList, sortCriteria);
            }
        }

        //put search result here
        boolean isDeptSame, isAySame, isNameIncluded;
        Iterator<Course> iter = allCourseList.iterator();
        while (iter.hasNext()){
            isDeptSame = false; isAySame = false; isNameIncluded = false;
            Course tempCourse = iter.next();
            // search through Department
            if (searchConditions.get("dept") != null) {
                if (searchConditions.get("dept").equals(tempCourse.department)){
                    isDeptSame = true;
                }
            } else {
                isDeptSame = true;
            }

            // search through Academic Year
            if (searchConditions.get("ay") != null) {
                if ( searchConditions.get("ay").equals(tempCourse.academicYear)){
                    isAySame = true;
                }
            } else {
                isAySame = true;
            }

            // search through Name
            if (searchConditions.get("name") != null) {
                isNameIncluded = isKeywordIncluded(tempCourse.courseName,(String) searchConditions.get("name"));
            } else {
                isNameIncluded = true;
            }

            if (isDeptSame && isAySame && isNameIncluded){
                searchResult.add(tempCourse);
            }
        }

        //Sort by sortCriteria (Defined a Method)
        if (sortCriteria == null){
            return searchResult;
        } else {
            return sortByCriteria(searchResult, sortCriteria);
        }
    }

    public int bid(int courseId, int mileage, String userId){
        // TODO Problem 2-2
        List<Course> allCourseList = readCourses("data/Courses/2020_Spring/");
        String userIdPath = String.format("data/Users/%s/",userId);

        //USERID_NOT_FOUND = -61
        if(new File(userIdPath).isDirectory() == false){
            return ErrorCode.USERID_NOT_FOUND;
        }

        //NO_COURSE_ID = -51
        boolean isCourseExist = false;
        Iterator<Course> iter = allCourseList.iterator();
        while(iter.hasNext()){
            if (iter.next().courseId == courseId){
                isCourseExist = true;
            }
        }
        if (isCourseExist == false) return ErrorCode.NO_COURSE_ID;

        //NEGATIVE_MILEAGE = -43
        if (mileage < 0) return ErrorCode.NEGATIVE_MILEAGE;

        //OVER_MAX_COURSE_MILEAGE = -43
        if (mileage > Config.MAX_MILEAGE_PER_COURSE) return ErrorCode.OVER_MAX_COURSE_MILEAGE;

        //OVER_MAX_MILEAGE=-41
        int sum = 0;
        Pair<Integer,List<Bidding>> pastBid = retrieveBids(userId);
        List<Bidding> pastBiddingList = pastBid.value;
        if (pastBid.key == ErrorCode.USERID_NOT_FOUND || pastBid.key == ErrorCode.IO_ERROR){
            sum = 0;
        } else {
            // Place a bid on a memory
            boolean didBidBefore = false, overWrite = false;
            Iterator<Bidding> iterator = pastBiddingList.iterator();
            while (iterator.hasNext()) {
                Bidding tempBidding = iterator.next();
                if (tempBidding.courseId == courseId) {
                    didBidBefore = true;
                    if (mileage == 0) {
                        iterator.remove();
                    } else {
                        tempBidding.mileage = mileage;
                        overWrite = true;
                    }
                }
            }
            // this block should not be executed if upper one is excuted.....
            if (!didBidBefore) {
                if (mileage != 0) {
                    pastBiddingList.add(new Bidding(courseId, mileage));
                } else {
                    // do nothing if mileage is 0
                    // just ignore the bid
                }
            }


            for (int i = 0; i < pastBiddingList.size(); i++) {
                sum += pastBiddingList.get(i).mileage;
            }
        }
        if (sum > Config.MAX_MILEAGE){
            return ErrorCode.OVER_MAX_MILEAGE;
        }

        //IO_ERROR = -10
        String userBidIdPath = String.format("data/Users/%s/bid.txt", userId);
        File userBid = new File(userBidIdPath);
        if(userBid.isFile() == false){
            return ErrorCode.IO_ERROR;
        }



        // Place a bid on a file
        try {
            FileWriter fileWriter = new FileWriter(userBid);
            for (int i = 0; i < pastBiddingList.size(); i++) {
                fileWriter.write(String.format("%d|%d\n",pastBiddingList.get(i).courseId,pastBiddingList.get(i).mileage));
            }
            fileWriter.close();
        } catch (IOException e){
            e.printStackTrace();
            return ErrorCode.IO_ERROR;
        }

        return ErrorCode.SUCCESS;

    }

    public Pair<Integer,List<Bidding>> retrieveBids(String userId){
        // TODO Problem 2-2

        String userIdPath = String.format("data/Users/%s/", userId);
        String userBidIdPath = String.format("data/Users/%s/bid.txt", userId);
        List<Bidding> userBiddingList = new ArrayList<>();
        File userBid = new File(userBidIdPath);

        if(new File(userIdPath).isDirectory() == false){
            return new Pair<>(ErrorCode.USERID_NOT_FOUND, userBiddingList);
        }

        if (!userBid.isFile()){
            return new Pair<>(ErrorCode.IO_ERROR, userBiddingList);
        }

        try{
            Scanner userBidInput = new Scanner(userBid);
            while(userBidInput.hasNext()){
                String[] tempBid = userBidInput.nextLine().split("[|]");
                userBiddingList.add(new Bidding(Integer.parseInt(tempBid[0]), Integer.parseInt(tempBid[1])));
            }
            userBidInput.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new Pair<>(ErrorCode.IO_ERROR, userBiddingList);
        }

        return new Pair<>(ErrorCode.SUCCESS,userBiddingList);
    }

    public boolean confirmBids(){
        // TODO Problem 2-3
        List<Course> allCourseList = readCourses("data/Courses/2020_Spring/");
        List<User> userList = new ArrayList<>();

        String usersPath = "data/Users/";
        File usersPathFile = new File(usersPath);
        String[] userIdList = usersPathFile.list();

        for (int i = 0; i < userIdList.length; i++) {
            Pair<Integer, List<Bidding>> tempPair = retrieveBids(userIdList[i]);
            userList.add(new User(userIdList[i], tempPair.value));
        }

        for (int i = 0; i < allCourseList.size(); i++) {
            List<Pair<User, Bidding>> tempBiddingList = new ArrayList<>();
            for (int j = 0; j < userList.size(); j++) {
                for (int k = 0; k < userList.get(j).biddingList.size(); k++) {
                    if (allCourseList.get(i).courseId == userList.get(j).biddingList.get(k).courseId){
                        tempBiddingList.add(new Pair<>(userList.get(j),userList.get(j).biddingList.get(k)));
                    }
                }
            }

            Collections.sort(tempBiddingList, new Comparator<Pair<User, Bidding>>() {
                @Override
                public int compare(Pair<User, Bidding> o1, Pair<User, Bidding> o2) {
                    if (o1.value.mileage > o2.value.mileage){
                        return -1;
                    } else if (o1.value.mileage < o2.value.mileage){
                        return 1;
                    } else {
                        if (o1.key.numTotalMileage > o2.key.numTotalMileage){
                            return 1;
                        } else if (o1.key.numTotalMileage < o2.key.numTotalMileage){
                            return -1;
                        } else {
                                if (o1.key.studentId.compareTo(o2.key.studentId) > 0){
                                    return 1;
                                } else if (o1.key.studentId.compareTo(o2.key.studentId) < 0) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }

                        }
                    }
            });


            if (tempBiddingList.size() > allCourseList.get(i).quota){
                tempBiddingList = tempBiddingList.subList(0,allCourseList.get(i).quota);
            } else {
                // do nothing
            }
            for (int j = 0; j < tempBiddingList.size(); j++) {
                tempBiddingList.get(j).key.registeredCourseList.add(allCourseList.get(i));
            }
        }

        for (User user : userList){
            String bidFilePath = usersPath + user.studentId+"/bid.txt";
            File bidFile = new File(bidFilePath);
            bidFile.delete();
            String coursesFilePath = usersPath + user.studentId+"/courses.txt";
            File coursesFile = new File(coursesFilePath);
            try {
                FileWriter fileWriter = new FileWriter(coursesFile);
                for (Course course : user.registeredCourseList){
                    fileWriter.write(course.courseId+"\n");
                }
                fileWriter.close();
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }

        }


        return true;
    }

    public Pair<Integer,List<Course>> retrieveRegisteredCourse(String userId){
        // TODO Problem 2-3
        String userIdPath = String.format("data/Users/%s/", userId);
        String userBidIdPath = String.format("data/Users/%s/courses.txt", userId);
        List<Course> allCourseList = readCourses("data/Courses/2020_Spring/");
        List<Course> userCourseList = new ArrayList<>();
        File userBid = new File(userBidIdPath);

        if(new File(userIdPath).isDirectory() == false){
            return new Pair<>(ErrorCode.USERID_NOT_FOUND, userCourseList);
        }

        if (!userBid.isFile()){
            return new Pair<>(ErrorCode.IO_ERROR, userCourseList);
        }

        try{
            Scanner userCourseInput = new Scanner(userBid);
            while(userCourseInput.hasNext()){
                /*
                * Should write a code that reads the information from course.txt
                * */
                int tempCourseId = Integer.parseInt(userCourseInput.nextLine());
                for (Course course : allCourseList){
                    if (course.courseId == tempCourseId){
                        userCourseList.add(course);
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Pair<>(ErrorCode.SUCCESS, userCourseList);

//        return new Pair<>(ErrorCode.IO_ERROR,new ArrayList<>());
    }

    List<Course> readCourses (String courseDataPath) {
        List<Course> courseList = new ArrayList<>();

        File collegePath = new File(courseDataPath);
        String[] collegeList = collegePath.list();

        for (String str : collegeList) {
            File coursePath = new File(courseDataPath + str);
            String[] collegeCourseList = coursePath.list();
            for (String id : collegeCourseList){
                File courseId = new File(courseDataPath + str + "/" + id);
                try {
                    Scanner input = new Scanner(courseId);
                    while (input.hasNext()){
                        String[] courseInfo = input.nextLine().split("[|]");
                        courseList.add(
                                new Course(Integer.parseInt(id.split("[.]")[0]), str,courseInfo[0],courseInfo[1],
                                Integer.parseInt(courseInfo[2]), courseInfo[3], Integer.parseInt(courseInfo[4]),
                                courseInfo[5], courseInfo[6], Integer.parseInt(courseInfo[7]))  );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(courseList);
        return courseList;
    }


    List<Course> sortByCriteria(List<Course> searchResult, String sortCriteria){
        if (sortCriteria.equals("id")){
        Collections.sort(searchResult);
        } else if (sortCriteria.equals("name")) {
            Collections.sort(searchResult, new Comparator<Course>() {
                @Override
                public int compare(Course o1, Course o2) {
                    if(o1.courseName.compareTo(o2.courseName) == 0) {
                        if (o1.courseId == o2.courseId){
                            return 0;
                        } else if (o1.courseId > o2.courseId) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {return o1.courseName.compareTo(o2.courseName);}
                }
            });
        } else if (sortCriteria.equals("dept")) {
            Collections.sort(searchResult, new Comparator<Course>() {
                @Override
                public int compare(Course o1, Course o2) {
                    if(o1.department.compareTo(o2.department) == 0) {
                        if (o1.courseId == o2.courseId){
                            return 0;
                        } else if (o1.courseId > o2.courseId) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {return o1.department.compareTo(o2.department);}
                }
            });
        } else if (sortCriteria.equals("ay")) {
            Collections.sort(searchResult, new Comparator<Course>() {
                @Override
                public int compare(Course o1, Course o2) {
                    if(o1.academicYear == o2.academicYear) {
                        if (o1.courseId == o2.courseId){
                            return 0;
                        } else if (o1.courseId > o2.courseId) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {return o1.academicYear - o2.academicYear;}
                }
            });
        }
        return searchResult;

    }

    boolean isKeywordIncluded (String courseName, String searchKeyword){
        List<String> courseNameSplit = new ArrayList<>(Arrays.asList(courseName.split(" ")));
        List<String> searchKeywordSplit = new ArrayList<>(Arrays.asList(searchKeyword.split(" ")));

        int flag = 1;
        for (int i = 0; i < searchKeywordSplit.size(); i++) {
            if (!courseNameSplit.contains(searchKeywordSplit.get(i))){
                flag *= 0;
            }
        }

        if (flag == 1) {
            return true;
        } else {
            return false;
        }

    }

}