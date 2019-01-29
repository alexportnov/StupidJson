/*
 * This work contains files distributed in Android, such files Copyright (C) 2016 The Android Open Source Project
 *
 * and are Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


package com.stupidjson.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.stupidjson.StupidJson;

import java.util.ArrayList;
import java.util.List;

public class SimpleActivity extends Activity
{
    private static final String TAG = "StupidJsonExample";

    /******************************************************************/
    // you have to make sure those are not optimized out if you use Proguard
    public static class TestClass
    {
        public enum TestEnum
        {
            ONE,
            // this will change the value of this enum
            @StupidJson.NameOverride("2")
            TWO,
        }

        public static class TestClassInner
        {
            private double d = 0.6;
        }

        public int t1 = 55;
        public boolean t2 = true;
        private Integer[] t3 = new Integer[]{1, 2, 3};
        protected String[] t4 = new String[]{"t1", "t2", "t3"};
        public List<String> t5 = new ArrayList<>();
        public TestEnum t6 = TestEnum.TWO;
        public TestEnum t7 = TestEnum.ONE;
        public TestClassInner t8 = new TestClassInner();

        // this field is ignored
        @StupidJson.Ignore
        public float t9;

        // this field will be saved/loaded as t3
        @StupidJson.NameOverride("test6")
        public float t10 = 0.5f;

        // strings are escaped for '"' and '\' chars
        public String t11 = "\"test\"";

        // do not escape this string - useful if you are saving json string inside json object
        @StupidJson.DontQuote
        public String t12 = "{\"test\":\"\"}";

        @Override
        public String toString()
        {
            return " t1=" + t1 +
                    " t2=" + t2 +
                    " t3=" + t3.length +
                    " t4=" + t4.length +
                    " t5=" + t5 +
                    " t6=" + t6 +
                    " t7=" + t7 +
                    " t8=" + t8.d +
                    " t9=" + t9 +
                    " t10=" + t10 +
                    " t11=" + t11 +
                    " t12=" + t12;
        }
    }

    /******************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        // to JSON
        TestClass c = new TestClass();
        String s = StupidJson.toJson(c);
        Log.d(TAG, s);

        // from JSON
        TestClass c1 = StupidJson.fromJson(s, TestClass.class);
        Log.d(TAG, c1.toString());

        // we can do it with compact types too
        Integer[] testList = new Integer[] { 1, 3, 6};
        String s1 = StupidJson.toJson(testList);
        Log.d(TAG, s1);

        Integer[] testList1 = StupidJson.fromJson(s1, Integer[].class);
        Log.d(TAG, "" + testList1[1]);
    }
}
