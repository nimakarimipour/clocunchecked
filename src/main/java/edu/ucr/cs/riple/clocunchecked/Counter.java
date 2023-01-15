/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.clocunchecked;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Counter {

  public final String root;
  public final List<Method> methods;

  public static final AnnotationExpr nullUnmarkedAnnotationExpr =
      new MarkerAnnotationExpr("NullUnmarked");

  public Counter(String root) {
    this.root = root;
    this.methods = new ArrayList<>();
  }

  public void count() {
    try (Stream<Path> paths = Files.walk(Paths.get(root))) {
      paths
          .filter(path -> path.toFile().getName().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  List<String> lines = Files.readAllLines(path);
                  CompilationUnit c = StaticJavaParser.parse(path);
                  c.stream()
                      .filter(node -> node instanceof MethodDeclaration)
                      .map(node -> (MethodDeclaration) node)
                      .filter(Counter::hasNullUnmarkedAnnotation)
                      .filter(methodDeclaration -> methodDeclaration.getRange().isPresent())
                      .forEach(
                          methodDeclaration ->
                              methods.add(new Method(lines, methodDeclaration.getRange().get())));
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
      countLines();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void countLines() {
    try {
      Path temp = Files.createTempFile("Clock", ".java");
      List<String> lines =
          methods.stream().flatMap(method -> method.lines.stream()).collect(Collectors.toList());
      lines.add(0, "public class GIANT {");
      lines.add("}");
      Files.write(temp, lines, Charset.defaultCharset());
      ProcessBuilder builder = new ProcessBuilder().command("cloc", temp.toString());
      Process p = builder.start();
      String result = new String(p.getInputStream().readAllBytes());
      System.out.println(result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean hasNullUnmarkedAnnotation(MethodDeclaration methodDeclaration) {
    return methodDeclaration.getAnnotations().contains(nullUnmarkedAnnotationExpr);
  }
}
