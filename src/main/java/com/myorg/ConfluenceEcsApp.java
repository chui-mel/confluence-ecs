package com.myorg;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class ConfluenceEcsApp {
    public static void main(final String[] args) {
        App app = new App();

        new ConfluenceEcsStack(app, "ConfluenceEcsStack");

        app.synth();
    }
}
