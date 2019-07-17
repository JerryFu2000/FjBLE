/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.clj.blesample.hex;

//注意：在hex类中只有方法，且不需要new出实例，就可以使用方法，所以方法前面必须用static来修饰
//static用来修饰成员方法，将其变为类方法，这样后续可以直接使用“类名.方法名”的方式调用，常用于工具类

import java.util.ArrayList;
import java.util.List;

public class hex {

    //=================================================================================
    //因为采用"类名.方法名"的方式调用，所以方法必须被声明为static
    //=================================================================================
    //计算hex累加和
    //入口：s_i --- 待计算累加和的字符串(必须每2个字符用“逗号或空格”隔开，作为1个字节，字符只能是0~F，大小写无关)
    //出口：返回值s_sum --- 8位hex的字符(大写,不足2位则前补0)
    //例如：s_i = "12,34,56,78,9a,bc,de,f0" 或者 "12 34 56 78 9a bc de f0" ,返回值s_sum = 00000438
    public static String string_8hexsum(String s_i){
        String s_sum;
        String[] sa_byte;
        int s32_sum, s32_i, s32_Unsigned8;

        //将s_i全部转大写，并用“逗号或空格”分割到数组sa_byte中，因此数组每个元素为2位hex字符
        s_i.toUpperCase();
        if(s_i.charAt(2)==','){ sa_byte = s_i.split(","); }
        else { sa_byte = s_i.split(" "); }

        //将字节数组的每一个元素转换为"s8&0xFF"的无符号数s32_Unsigned8，但实际这个u8是保存在s32内的
        s32_sum = 0;    //将校验和清零
        //历遍数组sa_byte，并同时进行累加校验和
        for(s32_i=0; s32_i<sa_byte.length; s32_i++){
            System.out.println(sa_byte[s32_i]);
            //由于Java没有无符号类型，因此必须如下处理8位无符号数的累加
            //先将2位hex字符转换成十六进制的数值，该数值是存放在byte类型(-127~+127)中
            //然后将该byte类&0xFF后存入int类型(有符号32位)中，这样后续进行累加就不会出错了
            s32_Unsigned8 = hex2_to_s8(sa_byte[s32_i]) & 0xFF;
            //进行累加
            s32_sum += s32_Unsigned8;
        }
        System.out.println("s32_sum=" + s32_sum);
        //将累加和转换为8位hex字符
        s_sum = s32_to_hex8(s32_sum);
        System.out.println("s_sum=" + s_sum);
        return(s_sum);
    }
	

    //入口：hexstr --- 代表hex的字符串
    //出口：s8list --- List<Byte>存有message中连续2个hex字符转换为1Byte的集合
    //例如：hexstr=55AA0100    返回s8list[0]=55 s8list[1]=AA
    public static List<Byte> hexstr_to_s8list(String hexstr){
        List<Byte> s8list = new ArrayList<>();

        for(int i=0; i<hexstr.length(); i+=2){
            String ss = hexstr.substring(i, i+2);
            s8list.add(hex2_to_s8(ss));
        }
        return s8list;
    }

    //=================================================================================
    //入口：bytes[] --- 待转换的字节数组
    //出口：返回值s_i --- hex的字符(大写,不足2位则前补0)
    //例如：bytes[0] = 154(十进制) bytes[1] = 155(十进制)   返回值s_i = 9A 9B
    public static String byteArray_to_String(byte[] bytes){
        String s_i = s8_to_hex2(bytes[0]) + " ";

        for (int i=1; i<bytes.length; i++){
            s_i += s8_to_hex2(bytes[i]);
            s_i += " ";
        }

        return (s_i);
    }

    //=================================================================================
    //入口：s8_i --- 待转换的1个字节
    //出口：返回值s_i --- 2位hex的字符(大写,不足2位则前补0)
    //例如：s8_i = 154(十进制)   返回值s_i = 9A
    public static String s8_to_hex2(byte s8_i){
        //步骤1：将byte转换成int
        int s32_i = s8_i & 0xFF;
        //步骤2：利用Integer.toHexString(int)来转换成16进制字符串
        String s_i = Integer.toHexString(s32_i);
        //步骤3：若只有1个字符,则前面补字符'0'
        if(s_i.length()==1){s_i = '0' + s_i;}
        //步骤4：全部改为大写
        s_i = s_i.toUpperCase();	
        return (s_i);
    }	

    //入口：s16_i --- 待转换的2个字节
    //出口：返回值s_i --- 4位hex的字符(大写,不足4位则前补0)
    //例如：s8_i = 154(十进制)   返回值s_i = 009A
    public static String s16_to_hex4(short s16_i){
        //步骤1：将byte转换成int
        int s32_i = s16_i & 0xFFFF;		
        //步骤2：利用Integer.toHexString(int)来转换成16进制字符串
        String s_i = Integer.toHexString(s32_i);
        //步骤3：不足4位则前补0
        for(int s32_j=4-s_i.length(); s32_j>0; s32_j--){
            s_i = '0' + s_i;		
        }
        //步骤4：全部改为大写
        s_i = s_i.toUpperCase();	
        return (s_i);
    }
	
    //入口：s32_i --- 待转换的4个字节
    //出口：返回值s_i --- 8位hex的字符(大写,不足8位则前补0)
    //例如：s8_i = 154(十进制)   返回值s_i = 0000_009A
    public static String s32_to_hex8(int s32_i){
        //步骤1：利用Integer.toHexString(int)来转换成16进制字符串
        String s_i = Integer.toHexString(s32_i);
        //步骤2：不足8位则前补0
        for(int s32_j=8-s_i.length(); s32_j>0; s32_j--){
            s_i = '0' + s_i;		
        }
        //转大写
        s_i = s_i.toUpperCase();	
        return (s_i);
    }

    //入口：s64_i --- 待转换的8个字节 
    //出口：返回值s_i --- 16位hex的字符(大写,不足16位则前补0)
    //例如：s8_i = 154(十进制)   返回值s_i = 0000_0000_0000_009A
    public static String s64_to_hex16(long s64_i){	
        //步骤1：利用Long.toHexString(int)来转换成16进制字符串
        String s_i = Long.toHexString(s64_i);
        //步骤2：不足16位则前补0
        for(int s32_j=16-s_i.length(); s32_j>0; s32_j--){
            s_i = '0' + s_i;		
        }
        //步骤3：全部改为大写
        s_i = s_i.toUpperCase();	
        return (s_i);
    }

	
//=================================================================================
    //入口：u16_i --- 0~F中的一个
    //出口：返回值byte(s8) --- 0~15中的一个,否则返回-1
    //例如：u16_i = "0"   返回值 = 0
    //      u16_i = "9"   返回值 = 9
    //      u16_i = "A"   返回值 = 10
    //      u16_i = "F"   返回值 = 15
    //      u16_i = 其他  返回值 = -1
    public static byte charToByte(char u16_i){
        //找出u16_i在字典中的索引号(逆向查表)，然后返回
        return (byte) "0123456789ABCDEF".indexOf(u16_i);   
    }
 	
 	
    //入口：s_i --- 2位hex字符串
    //出口：返回值s8_i --- 转换出来的1个字节数值	
    //例如：s_i = "ab"   返回值s8_i = 0xab
    public static byte hex2_to_s8(String s_i){
        //步骤1：全部改为大写
        s_i = s_i.toUpperCase();
        //步骤2：将字符串转为字符数组
        char[] hexChars = s_i.toCharArray();
        //步骤3：用字符逆向查表，获得索引号(即为对应的数值)，然后移位后进行组合，得到结果
        byte s8_i = (byte) ((byte)charToByte(hexChars[0]) << 4 | 
                            (byte)charToByte(hexChars[1]));
        return (s8_i);
    }

    //入口：s_i --- 4位hex字符串
    //出口：返回值s16_i --- 转换出来的2个字节
    //例如：s_i = "abcd"   返回值s16_i = 0xabcd
    public static short hex4_to_s16(String s_i){
        //步骤1：全部改为大写
        s_i = s_i.toUpperCase();
        //步骤2：将字符串转为字符数组
        char[] hexChars = s_i.toCharArray();
        //步骤3：用字符逆向查表，获得索引号(即为对应的数值)，然后移位后进行组合，得到结果
        short s16_i = (short) ((short)charToByte(hexChars[0])<<12 | 
                               (short)charToByte(hexChars[1])<<8 | 
                               (short)charToByte(hexChars[2])<<4 | 
                               (short)charToByte(hexChars[3]));
        return (s16_i);
    }
		
    //入口：s_i --- 8位hex字符串
    //出口：返回值s32_i --- 转换出来的4个字节	
    //例如：s_i = "abcdef01"   返回值s32_i = 0xabcdef01
    public static int hex8_to_s32(String s_i){
        //步骤1：全部改为大写
        s_i = s_i.toUpperCase();
        //步骤2：将字符串转为字符数组
        char[] hexChars = s_i.toCharArray();
        //步骤3：用字符逆向查表，获得索引号(即为对应的数值)，然后移位后进行组合，得到结果
        int s32_i = (int) ((int)charToByte(hexChars[0])<<28 | 
                           (int)charToByte(hexChars[1])<<24 | 
                           (int)charToByte(hexChars[2])<<20 | 
                           (int)charToByte(hexChars[3])<<16 | 
                           (int)charToByte(hexChars[4])<<12 | 
                           (int)charToByte(hexChars[5])<<8 | 
                           (int)charToByte(hexChars[6])<<4 | 
                           (int)charToByte(hexChars[7]));
        return (s32_i);
    }
		
    //入口：s_i --- 16位hex字符串
    //出口：返回值s64_i --- 转换出来的8个字节
    //例如：s_i = "123456789abcdef0"   返回值s64_i = 0x123456789abcdef0
    public static long hex16_to_s64(String s_i){
        //步骤1：全部改为大写
        s_i = s_i.toUpperCase();
        //步骤2：将字符串转为字符数组
        char[] hexChars = s_i.toCharArray();
        //步骤3：用字符逆向查表，获得索引号(即为对应的数值)，然后移位后进行组合，得到结果
        long s64_i = (long)((long)charToByte(hexChars[0])<<60L | 
                            (long)charToByte(hexChars[1])<<56L | 
                            (long)charToByte(hexChars[2])<<52L | 
                            (long)charToByte(hexChars[3])<<48L | 
                            (long)charToByte(hexChars[4])<<44L | 
                            (long)charToByte(hexChars[5])<<40L | 
                            (long)charToByte(hexChars[6])<<36L | 
                            (long)charToByte(hexChars[7])<<32L |
                            (long)charToByte(hexChars[8])<<28L | 
                            (long)charToByte(hexChars[9])<<24L | 
                            (long)charToByte(hexChars[10])<<20L | 
                            (long)charToByte(hexChars[11])<<16L | 
                            (long)charToByte(hexChars[12])<<12L | 
                            (long)charToByte(hexChars[13])<<8L | 
                            (long)charToByte(hexChars[14])<<4L | 
                            (long)charToByte(hexChars[15]));
        return (s64_i);
    }   
    
    //将unicode字符串转换为ascii字符串
    //Unicode格式：1个字符占用2个字节
    //  （它在ascii字符基础上多了1个高8位字节，这个高8位字节位于奇数序列中，
    //    若需转换为ASCII字符，只需忽略这个高8位字节）
    //ASCII格式：1个字符占用1个字节
    public static String unicode2String(String unicode) {
        StringBuilder string = new StringBuilder();
        
        byte[] byte_unicode = unicode.getBytes();
        for (int i = 0; i < byte_unicode.length; i+=2) {
            // 追加成string
            string.append((char) byte_unicode[i]);
        }
        return string.toString();
    }    
    
}
