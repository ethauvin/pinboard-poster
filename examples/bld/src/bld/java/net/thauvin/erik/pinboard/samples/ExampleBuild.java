package net.thauvin.erik.pinboard.samples;

import rife.bld.BaseProject;
import rife.bld.BuildCommand;
import rife.bld.extension.CompileKotlinOperation;
import rife.bld.extension.kotlin.CompileOptions;
import rife.bld.operations.RunOperation;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.compile;

public class ExampleBuild extends BaseProject {
    public ExampleBuild() {
        pkg = "net.thauvin.erik.pinboard.samples";
        name = "Example";
        version = version(0, 1, 0);

        mainClass = pkg + ".KotlinExampleKt";

        javaRelease = 17;
        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL, CENTRAL_SNAPSHOTS);

        scope(compile)
                .include(dependency("net.thauvin.erik", "pinboard-poster", version(1, 2, 1, "SNAPSHOT")));
    }

    public static void main(String[] args) {
        new ExampleBuild().start(args);
    }

    @Override
    public void compile() throws Exception {
        var op = new CompileKotlinOperation().fromProject(this);
        op.compileOptions().verbose(true);
        op.execute();

        // Also compile the Java source code
        super.compile();
    }

    @BuildCommand(value = "run-java", summary = "Runs the Java example")
    public void runJava() throws Exception {
        new RunOperation()
                .fromProject(this)
                .mainClass(pkg + ".JavaExample")
                .execute();
    }
}
