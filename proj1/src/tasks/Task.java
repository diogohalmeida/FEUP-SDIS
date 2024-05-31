package tasks;

import communication.messages.Message;

public abstract class Task {
    Message message;

    public Task(Message message){
        this.message = message;
    }

    public abstract void start();
}
