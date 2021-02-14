package com.example.locationbaseddiary;

public class TaskItem {

    private String Description;
    private String DateTime_Task_Creation;
    private String Task_Classname;

    public TaskItem() {}
    public TaskItem(String Description, String DateTime_Task_Creation, String Task_Classname) {
        this.Description = Description;
        this.DateTime_Task_Creation = DateTime_Task_Creation;
        this.Task_Classname = Task_Classname;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getDateTime_Task_Creation() {
        return DateTime_Task_Creation;
    }

    public void setDateTime_Task_Creation(String dateTime_Task_Creation) {
        DateTime_Task_Creation = dateTime_Task_Creation;
    }

    public String getTask_Classname() {
        return Task_Classname;
    }

    public void setTask_Classname(String task_Classname) {
        Task_Classname = task_Classname;
    }
}
