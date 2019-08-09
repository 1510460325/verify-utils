package cn.wzy.verifyUtils.Check;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.wzy.verifyUtils.annotation.Verify;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author WangZY
 * @since  2019/6/6 10:42
 * @version 1.0
 **/
@Aspect
public class VerifyAspect {


    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyAspect.class);

    static {
        if (LOGGER.isDebugEnabled()) {
            System.err.println(" ┏┓　　　┏┓\n" +
                    " ┏┛┻━━━┛┻┓\n" +
                    " ┃　　　━　　　┃\n" +
                    " ┃　┳┛　┗┳　┃\n" +
                    " ┃　　　┻　　　┃\n" +
                    " ┗━┓　　　┏━┛\n" +
                    " ┃　　　┃ 神兽温馨提示：　　　　　　　　\n" +
                    " ┃　　　┃     参数判断不到位，测试小妹两行泪！\n" +
                    " ┃　　　┗━━━┓\n" +
                    " ┃　　　　　　　┣┓\n" +
                    " ┃　　　　　　　┏┛\n" +
                    " ┗┓┓┏━┳┓┏┛\n" +
                    " ┃┫┫　┃┫┫\n" +
                    " ┗┻┛　┗┻┛\n" +
                    "  我是参数校验插件自带坐骑\n" +
                    "  我只会在项目dubug模式初始化的时候出来");
        }
    }

    @Before("@annotation(verify)")
    public void verify(JoinPoint joinPoint, Verify verify) {
        //所有的参数和参数名
        Object[] values = joinPoint.getArgs();
        if (values.length == 0) {
            return;
        }
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        //未开启mvn保留方法名配置
        if (names == null) {
            throw new IllegalArgumentException("此插件基于cglib代理，请设置代理为cglib代理\n" +
                    "<aop:aspectj-autoproxy proxy-target-class=\"true\"/>");
        }
        JSONObject data = new JSONObject();
        for (int i = 0; i < names.length; i++) {
            data.put(names[i], values[i]);
        }
        String json = JSON.toJSONString(data);
        data = JSON.parseObject(json, JSONObject.class);
        String[] notBlank = verify.notNull();
        if (!allNotNull(notBlank, values, names)) {
            for (String pattern : notBlank) {
                pattern = transform(pattern);
                checkNull(pattern, data);
            }
        }
        String[] strSizeLimit = verify.sizeLimit();
        for (String pattern : strSizeLimit) {
            pattern = transform(pattern);
            SizeLimit limit = resolvePattern(pattern);
            checkSize(limit, data);
        }
        String[] regex = verify.regex();
        for (String pattern : regex) {
            checkRegex(pattern, data);
        }
        String[] numberLimit = verify.numberLimit();
        for (String pattern : numberLimit) {
            pattern = transform(pattern);
            SizeLimit limit = resolvePattern(pattern);
            checkNumberLimit(limit, data);
        }
    }

    /**
     * 所有都不能为空
     *
     * @param notBlank
     * @param values
     * @param names
     */
    private boolean allNotNull(String[] notBlank, Object[] values, String[] names) {
        if (notBlank.length == 1 && notBlank[0].trim().equals("*")) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException(names[i] + "不能为空");
                }
                if (values[i] instanceof String && !isNotBlank((String) values[i])) {
                    throw new IllegalArgumentException(names[i] + "不能为空");
                }
                if (values[i] instanceof Collection) {
                    Collection<?> val = ((Collection) values[i]);
                    for (Object obj : val) {
                        if (obj == null) {
                            throw new IllegalArgumentException(names[i] + " 元素不能为空");
                        } else if (obj instanceof String && !isNotBlank((String) obj)) {
                            throw new IllegalArgumentException(names[i] + " 元素不能为空");
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 检查数字是否合法
     *
     * @param limit
     * @param data
     */
    private void checkNumberLimit(SizeLimit limit, JSONObject data) {
        Object value = valueOfPattern(limit.name, data);
        if (value == null) {
            throw new IllegalArgumentException(limit.name + "不能为空");
        }
        if (value instanceof Integer) {
            Integer val = ((Integer) value);
            Integer low = new Integer(limit.low);
            Integer high = new Integer(limit.high);
            if (low > val || high < val) {
                throw new IllegalArgumentException(String.format("%s 取值越界,%s不在[%s,%s]内",
                        limit.name, val, limit.low, limit.high));
            }
        } else if (value instanceof BigDecimal) {
            BigDecimal val = ((BigDecimal) value);
            BigDecimal low = new BigDecimal(limit.low);
            BigDecimal high = new BigDecimal(limit.high);
            if (low.compareTo(val) > 0 || high.compareTo(val) < 0) {
                throw new IllegalArgumentException(String.format("%s 取值越界,%s不在[%s,%s]内",
                        limit.name, val, limit.low, limit.high));
            }
        }
    }


    /**
     * 验证正则表达式
     *
     * @param pattern
     */
    private void checkRegex(String pattern, JSONObject data) {
        String[] param = pattern.split("=>");
        if (param.length != 2) {
            throw new IllegalArgumentException(pattern + " 配置有误");
        } else {
            Object value = valueOfPattern(param[0], data);
            if (value == null) {
                throw new IllegalArgumentException(pattern + " 不能为空");
            } else if (!(value instanceof String)) {
                throw new IllegalArgumentException(String.format("参数%s不是字符串", param[0]));
            }
            String val = ((String) value);
            if (!Pattern.matches(param[1], val)) {
                throw new IllegalArgumentException(String.format("参数%s不符合正则语法:%s", param[0], param[1]));
            }
        }
    }

    /**
     * 验证长度是否满足条件
     *
     * @param limit
     * @param data
     */
    private void checkSize(SizeLimit limit, JSONObject data) {
        Object value = valueOfPattern(limit.name, data);
        if (value == null) {
            throw new IllegalArgumentException(limit.name + "不能为空");
        }
        if (value instanceof String) {
            String val = (String) value;
            if (!(val.length() >= Integer.valueOf(limit.low) && val.length() <= Integer.valueOf(limit.high))) {
                throw new IllegalArgumentException(String.format("%s size越界,size:%s不在[%s,%s]内",
                        limit.name, val.length(), limit.low, limit.high));
            }
        } else if (value instanceof Collection) {
            Collection val = (Collection) value;
            if (!(val.size() >= Integer.valueOf(limit.low) && val.size() <= Integer.valueOf(limit.high))) {
                throw new IllegalArgumentException(String.format("%s size越界,size:%s不在[%s,%s]内",
                        limit.name, val.size(), limit.low, limit.high));
            }
        }
    }

    /**
     * 从json中获取参数值
     *
     * @param pattern
     * @param data
     * @return Object
     */
    private Object valueOfPattern(String pattern, JSONObject data) {
        JSONObject root = data;
        if (data.containsKey(pattern)) {
            return data.get(pattern);
        }
        String[] params = pattern.split("\\.");
        Object value = null;
        for (String param : params) {
            value = data.get(param);
            if (value == null)
                return null;
            if (value instanceof JSONObject) {
                data = (JSONObject) value;
            }
        }
        root.put(pattern, value);
        return value;
    }

    /**
     * 压缩字符串
     *
     * @param str
     * @return
     */
    private String transform(String str) {
        while (str.contains(" ")) {
            str = str.replace(" ", "");
        }
        return str;
    }

    /**
     * 检验是否为空
     *
     * @param pattern
     * @param data
     */
    private void checkNull(String pattern, JSONObject data) {
        Object value = valueOfPattern(pattern, data);
        if (value == null) {
            throw new IllegalArgumentException(pattern + " 不能为空");
        } else if (value instanceof String && !isNotBlank((String) value)) {
            throw new IllegalArgumentException(pattern + " 不能为空");
        }
        if (value instanceof Collection) {
            Collection<?> val = ((Collection) value);
            for (Object obj : val) {
                if (obj == null) {
                    throw new IllegalArgumentException(pattern + " 元素不能为空");
                } else if (obj instanceof String && !isNotBlank((String) obj)) {
                    throw new IllegalArgumentException(pattern + " 元素不能为空");
                }
            }
        }
    }

    private boolean isNotBlank(String s) {
        return !(s == null || s.trim().length() == 0);
    }

    /**
     * 获取参数的长度限制配置
     *
     * @param pattern
     * @return
     */
    private SizeLimit resolvePattern(String pattern) {
        SizeLimit res = new SizeLimit();
        int middle = pattern.indexOf("[");
        if (middle == -1) {
            throw new IllegalArgumentException(pattern + " 配置有误");
        }
        res.name = pattern.substring(0, middle);

        int index = pattern.indexOf(",");
        if (index == -1) {
            throw new IllegalArgumentException(pattern + " 配置有误");
        }
        res.low = pattern.substring(middle + 1, index);
        res.high = pattern.substring(index + 1, pattern.length() - 1);
        return res;
    }

    private static class SizeLimit {
        String name;
        String low;
        String high;
        boolean reverse;
    }
}
