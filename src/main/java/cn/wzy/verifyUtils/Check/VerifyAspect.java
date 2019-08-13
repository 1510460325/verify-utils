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
            System.err.println(
            	"      ┏┛ ┻━━━━━┛ ┻┓\n" +
				"      ┃　　　━　　 ┃\n" +
				"      ┃　┳┛　  ┗┳　┃\n" +
				"      ┃　　　　　　┃\n" +
				"      ┃　　　┻　　 ┃\n" +
				"      ┗━┓　　　┏━━┛\n" +
				"        ┃　　　┃   \n" +
				"        ┃　　　┃   no bug!\n" +
				"        ┃　　　┗━━━━━━━━━┓\n" +
				"        ┃　verify-utils  ┣┓\n" +
				"        ┃　　　　        ┏┛\n" +
				"        ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛\n" +
				"          ┃ ┫ ┫   ┃ ┫ ┫\n" +
				"          ┗━┻━┛   ┗━┻━┛");
        }
    }

    @Before("@annotation(verify)")
    public void verify(JoinPoint joinPoint, Verify verify) {
        Object[] values = joinPoint.getArgs();
        if (values.length == 0) {
            return;
        }
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        if (names == null) {
            throw new IllegalArgumentException("This plug-in is based on cglib proxy. Please set the proxy to cglib proxy.\n" +
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
     * all not be null
     *
     * @param notBlank
     * @param values
     * @param names
     */
    private boolean allNotNull(String[] notBlank, Object[] values, String[] names) {
        if (notBlank.length == 1 && notBlank[0].trim().equals("*")) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException(names[i] + " Can't be empty");
                }
                if (values[i] instanceof String && !isNotBlank((String) values[i])) {
                    throw new IllegalArgumentException(names[i] + " Can't be empty");
                }
                if (values[i] instanceof Collection) {
                    Collection<?> val = ((Collection) values[i]);
                    for (Object obj : val) {
                        if (obj == null) {
                            throw new IllegalArgumentException(names[i] + " Elements cannot be empty");
                        } else if (obj instanceof String && !isNotBlank((String) obj)) {
                            throw new IllegalArgumentException(names[i] + " Elements cannot be empty");
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * number limit
     *
     * @param limit
     * @param data
     */
    private void checkNumberLimit(SizeLimit limit, JSONObject data) {
        Object value = valueOfPattern(limit.name, data);
        if (value == null) {
            throw new IllegalArgumentException(limit.name + " Can't be empty");
        }
        if (value instanceof Integer) {
            Integer val = ((Integer) value);
            Integer low = new Integer(limit.low);
            Integer high = new Integer(limit.high);
            if (low > val || high < val) {
                throw new IllegalArgumentException(String.format("%s The value crosses the boundary and %s is not in [%s,%s]",
                        limit.name, val, limit.low, limit.high));
            }
        } else if (value instanceof BigDecimal) {
            BigDecimal val = ((BigDecimal) value);
            BigDecimal low = new BigDecimal(limit.low);
            BigDecimal high = new BigDecimal(limit.high);
            if (low.compareTo(val) > 0 || high.compareTo(val) < 0) {
                throw new IllegalArgumentException(String.format("%s The value crosses the boundary and %s is not in [%s,%s]",
                        limit.name, val, limit.low, limit.high));
            }
        }
    }


    /**
     * reg limit
     *
     * @param pattern
     */
    private void checkRegex(String pattern, JSONObject data) {
        String[] param = pattern.split("=>");
        if (param.length != 2) {
            throw new IllegalArgumentException(pattern + " Misconfiguration");
        } else {
            Object value = valueOfPattern(param[0], data);
            if (value == null) {
                throw new IllegalArgumentException(pattern + " Can't be empty");
            } else if (!(value instanceof String)) {
                throw new IllegalArgumentException(String.format("The parameter %s is not a string", param[0]));
            }
            String val = ((String) value);
            if (!Pattern.matches(param[1], val)) {
                throw new IllegalArgumentException(String.format("The parameter %s does not conform to the regular grammar:%s", param[0], param[1]));
            }
        }
    }

    /**
     * length limit
     *
     * @param limit
     * @param data
     */
    private void checkSize(SizeLimit limit, JSONObject data) {
        Object value = valueOfPattern(limit.name, data);
        if (value == null) {
            throw new IllegalArgumentException(limit.name + " Can't be empty");
        }
        if (value instanceof String) {
            String val = (String) value;
            if (!(val.length() >= Integer.valueOf(limit.low) && val.length() <= Integer.valueOf(limit.high))) {
                throw new IllegalArgumentException(String.format("%s crosses the boundary: %s is not in [%s,%s]",
                        limit.name, val.length(), limit.low, limit.high));
            }
        } else if (value instanceof Collection) {
            Collection val = (Collection) value;
            if (!(val.size() >= Integer.valueOf(limit.low) && val.size() <= Integer.valueOf(limit.high))) {
                throw new IllegalArgumentException(String.format("%s crosses the boundary: %s is not in [%s,%s]",
                        limit.name, val.size(), limit.low, limit.high));
            }
        }
    }

    /**
     * get data from json
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
     * trim the string
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
     * Can't be empty
     *
     * @param pattern
     * @param data
     */
    private void checkNull(String pattern, JSONObject data) {
        Object value = valueOfPattern(pattern, data);
        if (value == null) {
            throw new IllegalArgumentException(pattern + " Can't be empty");
        } else if (value instanceof String && !isNotBlank((String) value)) {
            throw new IllegalArgumentException(pattern + " Can't be empty");
        }
        if (value instanceof Collection) {
            Collection<?> val = ((Collection) value);
            for (Object obj : val) {
                if (obj == null) {
                    throw new IllegalArgumentException(pattern + " Elements cannot be empty");
                } else if (obj instanceof String && !isNotBlank((String) obj)) {
                    throw new IllegalArgumentException(pattern + " Elements cannot be empty");
                }
            }
        }
    }

    private boolean isNotBlank(String s) {
        return !(s == null || s.trim().length() == 0);
    }

    /**
     * parameters size limit
     *
     * @param pattern
     * @return
     */
    private SizeLimit resolvePattern(String pattern) {
        SizeLimit res = new SizeLimit();
        int middle = pattern.indexOf("[");
        if (middle == -1) {
            throw new IllegalArgumentException(pattern + " Misconfiguration");
        }
        res.name = pattern.substring(0, middle);

        int index = pattern.indexOf(",");
        if (index == -1) {
            throw new IllegalArgumentException(pattern + " Misconfiguration");
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
