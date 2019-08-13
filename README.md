## 参数校验工具包
工具说明：参数校验插件，注解式的标记参数的限制（为空、参数长度、取值范围），可以代替很多冗杂的if判断，比如：
~~~
    public void hello(String host, User user) {
		if (host == null || host.length() == 0) {
			throw new IllegalArgumentException("host can't be empty");
		}
		if (user == null || user.getFullName() == null || user.getFullName().length() == 0) {
			throw new IllegalArgumentException("user.fullName can't be empty");
		}
		System.out.println("====SUCCESS====");
    }
~~~
可以使用插件优化代码，增强可读性：
~~~
	@Verify(
		notNull = {"host", "user.fullName"}
	)
	public void hello(String host, User user) {
		System.out.println("====SUCCESS====");
	}
~~~
### 使用示例
#### 1.添加依赖
~~~
<dependency>
    <groupId>com.github.1510460325</groupId>
    <artifactId>verify-utils</artifactId>
    <version>0.0.1</version>
</dependency>
~~~
#### 项目中导入开关：@EnableVerify
~~~
@SpringBootApplication
@EnableVerify
public class ProducerApplication {
 
    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}
~~~
#### 方法中配置检验@Verify
~~~
@Verify(
        notNull = {"user","users"},
        sizeLimit = {"user.username [3,10]", "users [1,20]"},
        regex = {"user.username=>^[a-zA-Z0-9]{5,20}$"},
        numberLimit = {"count [1, 9999]"}
)
public String method1(User user, List<User> users, Date date, String name, Long time, Integer count, Double price) {
    return "SUCCESS";
}
~~~
配置说明：
* NotNull：参数为String[],传入多个参数名，***如果notNull = {"*"}表示所有参数都不为空***，检测dto对象***需要用'.'来标明作用域：user.username***
* SizeLimit传需要控制长度的参数（参数必须为String或者是集合对象Collection）规则：参数名+[start,end] (参数间可以有空格美化)
* regex正则匹配，规则：参数名=>正则语法(***参数间不能有多余空格***）
* numberLimit数字范围匹配（参数必须为Integer或者是Double）：规则：参数名+[start,end] (参数间可以有空格美化)