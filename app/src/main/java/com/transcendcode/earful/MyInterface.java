package com.transcendcode.earful;

public class MyInterface {

    DialogReturn dialogReturn;
    public int data;

    public interface DialogReturn {

        void onDialogCompleted(boolean answer);
    }

    public void setListener(DialogReturn dialogReturn) {
        this.dialogReturn = dialogReturn;
    }

    public DialogReturn getListener() {
        return dialogReturn;

    }
}