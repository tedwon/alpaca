package io.alpaca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Paths;

/**
 * https://repo1.maven.org/maven2/io/fabric8/kubernetes-openshift-uberjar/4.6.3/kubernetes-openshift-uberjar-4.6.3.jar
 * <p/>
 * https://get.jenkins.io/war-stable/2.263.4/jenkins.war
 */
public class AppTest {

    @Test
    @DisplayName("No pom.xml")
    public void testPomZero() throws Exception {
        URL resource = getClass().getClassLoader().getResource("guice-4.0.jar");
        String absolutePath = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        String[] args = {absolutePath};
        App.main(args);
    }

//    @Test
//    @DisplayName("bundled jar")
//    public void testBundledJar() throws Exception {
//        String[] args = {"/tmp/jenkins.war"};
//        App.main(args);
//    }
//
//    @Test
//    @DisplayName("uber jar")
//    public void testUberJar() throws Exception {
//        String[] args = {"/tmp/kubernetes-openshift-uberjar-4.6.3.jar"};
//        App.main(args);
//    }
}
