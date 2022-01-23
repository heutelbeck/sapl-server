# Text Editors for the SAPL DSL

This project provides text editors to be used with SAPL DSL. The following examples show its usage in a Vaadin application.

## General Requirements

The usage of this component requires at least Java 11.

### Vaadin 14 Web Application

The text editors are implemented as addons for a Vaadin 14 application. As such it is required that the target application uses at least a Vaadin 14. 

For a quick start the base application Vaadin Flow Quick Start can be used:

https://vaadin.com/docs/v14/flow/guide/quick-start

### Adding project reference

To add the text editors to the project first add the dependency to the project.
For Maven it will look like this:

```xml
<dependency>
    <groupId>io.sapl</groupId>
    <artifactId>sapl-editor-for-vaadin</artifactId>
    <version>2.0.0</version>
</dependency>
```

## SaplEditor

This class is used to edit policies written in the SAPL DSL. 

### Requirements

The editor requires the matching xtext-services and spring configurations to be available on the server. These can be set up as follows:

```java
import java.time.Clock;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.sapl.functions.FilterFunctionLibrary;
import io.sapl.functions.StandardFunctionLibrary;
import io.sapl.functions.TemporalFunctionLibrary;
import io.sapl.grammar.web.SAPLServlet;
import io.sapl.interpreter.InitializationException;
import io.sapl.interpreter.functions.AnnotationFunctionContext;
import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.interpreter.pip.AnnotationAttributeContext;
import io.sapl.interpreter.pip.AttributeContext;
import io.sapl.pip.TimePolicyInformationPoint;

@Configuration
@ComponentScan("io.sapl.grammar.ide.contentassist")
public class XtextServletConfiguration {

	@Bean
	public static ServletRegistrationBean<SAPLServlet> xTextRegistrationBean() {
		ServletRegistrationBean<SAPLServlet> registration = new ServletRegistrationBean<>(new SAPLServlet(),
				"/xtext-service/*");
		registration.setName("XtextServices");
		registration.setAsyncSupported(true);
		return registration;
	}

	@Bean
	public static FilterRegistrationBean<OrderedFormContentFilter> registration1(OrderedFormContentFilter filter) {
		FilterRegistrationBean<OrderedFormContentFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public FunctionContext functionContext() throws InitializationException {
		FunctionContext context = new AnnotationFunctionContext(new FilterFunctionLibrary(),
				new StandardFunctionLibrary(), new TemporalFunctionLibrary());
		return context;
	}

	@Bean
	public AttributeContext attributeContext() throws InitializationException {
		AnnotationAttributeContext context = new AnnotationAttributeContext();
		context.loadPolicyInformationPoint(new TimePolicyInformationPoint(Clock.systemUTC()));
		return context;
	}

}
```

The beans for `FunctionContext` and `AttributeContext` are only needed if the server environment has not defined any of their own yet. 

### Creation

The editor can simply be created and added like a normal UI element.
```java
    public MainView() {
    	SaplEditor saplEditor = new SaplEditor(new SaplEditorConfiguration());
    	saplEditor.setDocument("policy \"example\" permit");
    	add(saplEditor);
    }
```

### Configuration

Upon creation of the editor it can be configured with the configuration object, currently the following configuration properties are available:

* `hasLineNumbers` - Whether or not to display lineNumbers in the editor
* `textUpdateDelay` - The delay between the user stopped typing and the editor validating the inputed text

```java
SaplEditorConfiguration saplConfig = new SaplEditorConfiguration();
saplConfig.setHasLineNumbers(true);
saplConfig.setTextUpdateDelay(500);

saplEditor = new SaplEditor(saplConfig);
```

### Set and get the current Document

The current document can be set or get via the setter and getter of the editor.

```java
saplEditor.setDocument("policy \"set by Vaadin View after instantiation ->\\u2588<-\" permit");
String document = saplEditor.getDocument();
```

### DocumentChangedListener

The editor enables to set a listener that is notified every time the editor document changes.  The listener has to implement the ```DocumentChangedListener``` interface.

```java
public interface DocumentChangedListener {
	void onDocumentChanged(DocumentChangedEvent event);
}
```

```java
public class JavabasedViewView extends Div implements DocumentChangedListener {
	...
    saplEditor.addDocumentChangedListener(this);
    ...
    
    public void onDocumentChanged(DocumentChangedEvent event) {
		System.out.println("value changed: " + event.getNewValue());
	}
}
```

```java
public class JavabasedViewView extends Div {
	...
    saplEditor.addDocumentChangedListener(this::onDocumentChanged);
    ...
    
    public void onDocumentChanged(DocumentChangedEvent event) {
		System.out.println("value changed: " + event.getNewValue());
	}
}
```

### ValidationFinishedListener

The editor enables to set a listener that is notified every time the editor validation is executed. The listener has to implement the ```ValidationFinishedListener``` interface.

```java
public interface ValidationFinishedListener {
	void onValidationFinished(ValidationFinishedEvent event);
}
```

```java
public class JavabasedViewView extends Div implements ValidationFinishedListener {
	...
    saplEditor.addValidationFinishedListener(this);
    ...
    
	private void onValidationFinished(ValidationFinishedEvent event) {
		System.out.println("validation finished");
		Issue[] issues = event.getIssues();
		System.out.println("issue count: " + issues.length);
		for (Issue issue : issues) {
			System.out.println(issue.getDescription());
		}
	}
}
```

```java
public class JavabasedViewView extends Div {
	...
    saplEditor.addValidationFinishedListener(this::onValidationFinished);
    ...
    
	private void onValidationFinished(ValidationFinishedEvent event) {
		System.out.println("validation finished");
		Issue[] issues = event.getIssues();
		System.out.println("issue count: " + issues.length);
		for (Issue issue : issues) {
			System.out.println(issue.getDescription());
		}
	}
}
```

### Read-Only Mode

The editor can be set to a read-only mode that prevents the user from changing the set document in the UI.

```java
// prevent the user from changing the current document
saplEditor.setReadOnly(true);
// enable the user to change the current document again
saplEditor.setReadOnly(false);
// determine current state
Boolean isReadOnly = saplEditor.isReadOnly();
```

## JsonEditor

This class is used to write configuration files in JSON.

### Creation

The editor can simply be created and added like a normal UI element.
```java
public class JavabasedViewView extends Div {
    public JavabasedViewView() {
        setId("javabased-view-view");
        JsonEditor jsonEditor = new JsonEditor(new JsonEditorConfiguration());
        add(jsonEditor);
    }
}
```

### Configuration

Upon creation of the editor it can be configured with the configuration object, currently the following configuration properties are available:

* `hasLineNumbers` - Whether or not to display lineNumbers in the editor
* `textUpdateDelay` - The delay between the user stopped typing and the editor validating the inputed text

```java
JsonEditorConfiguration jsonEditorConfig = new JsonEditorConfiguration();
jsonEditorConfig.setHasLineNumbers(true);
jsonEditorConfig.setTextUpdateDelay(500);

JsonEditor jsonEditor = new JsonEditor(jsonEditorConfig);
add(jsonEditor)
```

### Set and get the current Document

The current document can be set or get via the setter and getter of the editor.

```java
jsonEditor.setDocument("[\r\n"
		+ " {\r\n"
		+ "  _id: \"post 1\",\r\n"
		+ "  \"author\": \"Bob\",\r\n"
		+ "  \"content\": \"...\",\r\n"
		+ "  \"page_views\": 5\r\n"
		+ " },\r\n"
		+ " {\r\n"
		+ "  \"_id\": \"post 2\",\r\n"
		+ "  \"author\": \"Bob\",\r\n"
		+ "  \"content\": \"...\",\r\n"
		+ "  \"page_views\": 9\r\n"
		+ " },\r\n"
		+ " {\r\n"
		+ "  \"_id\": \"post 3\",\r\n"
		+ "  \"author\": \"Bob\",\r\n"
		+ "  \"content\": \"...\",\r\n"
		+ "  \"page_views\": 8\r\n"
		+ " }\r\n"
		+ "]\r\n"
		+ "");
String document = jsonEditor.getDocument();
```


### DocumentChangedListener

The editor enables to set a listener that is notified every time the editor document changes.  The listener has to implement the ```DocumentChangedListener``` interface.

```java
public interface DocumentChangedListener {
	void onDocumentChanged(DocumentChangedEvent event);
}
```

```java
public class JavabasedViewView extends Div implements DocumentChangedListener {
	...
    jsonEditor.addDocumentChangedListener(this);
    ...
    
    public void onDocumentChanged(DocumentChangedEvent event) {
		System.out.println("value changed: " + event.getNewValue());
	}
}
```

```java
public class JavabasedViewView extends Div {
	...
    jsonEditor.addDocumentChangedListener(this::onDocumentChanged);
    ...
    
    public void onDocumentChanged(DocumentChangedEvent event) {
		System.out.println("value changed: " + event.getNewValue());
	}
}
```

### Read-Only Mode

The editor can be set to a read-only mode that prevents the user from changing the set document in the UI.

```java
// prevent the user from changing the current document
jsonEditor.setReadOnly(true);
// enable the user to change the current document again
jsonEditor.setReadOnly(false);
// determine current state
Boolean isReadOnly = jsonEditor.isReadOnly();
```