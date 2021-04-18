# An Editor for the SAPL DSL

This project provides Editors to be used with SAPL DSL. The following examples show its usage in a Vaadin application.

## SaplEditor

This class is used to edit policies written in the SAPL DSL. 

### Requirements

The editor requires the matching stext-services to be available on the server. These can be set up as follows:

```java
@Configuration
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

}
```

### Creation

The editor can simply be created and added like a normal UI element.
```java
public class JavabasedViewView extends Div {
    public JavabasedViewView() {
        setId("javabased-view-view");
        SaplEditor editor = new SaplEditor(new SaplEditorConfiguration());
        add(editor);
    }
}
```

### Configuration

Upon creation of the editor it can be configured with the configuration object, currently the following configuration properties are available:

* hasLineNumbers - Whether or not to display lineNumbers in the editor
* textUpdateDelay = 500 - The delay between the user stopped typing and the editor validating the inputed text

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

* hasLineNumbers - Whether or not to display lineNumbers in the editor
* textUpdateDelay = 500 - The delay between the user stopped typing and the editor validating the inputed text

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