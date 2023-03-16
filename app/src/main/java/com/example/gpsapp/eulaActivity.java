package com.example.gpsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class eulaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        TextView textView = findViewById(R.id.eulaText);

        String eula = "END USER LICENSE AGREEMENT (EULA) FOR SMART PARKING APPLICATION\n" +
                "\n" +
                "PLEASE READ THIS END USER LICENSE AGREEMENT (\"EULA\") CAREFULLY BEFORE USING THE SMART PARKING APPLICATION.\n" +
                "\n" +
                "1. GENERAL TERMS\n" +
                "This EULA is a legal agreement between you (either an individual or a single entity) and [Insert Company Name] (\"Company\"), the developer of the Smart Parking Application (\"Application\"). By using the Application, you agree to be bound by the terms of this EULA. If you do not agree to the terms of this EULA, do not use the Application.\n" +
                "\n" +
                "2. LICENSE GRANT\n" +
                "Subject to the terms and conditions of this EULA, Company hereby grants you a limited, non-exclusive, non-transferable, revocable license to use the Application solely for the purpose of locating available parking spots on a university campus.\n" +
                "\n" +
                "3. USER DATA\n" +
                "The Application may use your phone's location to determine available parking spots on a university campus. By using the Application, you agree to the collection, transmission, processing, and storage of your location data by the Application.\n" +
                "\n" +
                "4. PROPRIETARY RIGHTS\n" +
                "The Application is owned and operated by Company. The Application and its entire contents, features, and functionality (including but not limited to all information, software, text, displays, images, video, and audio) are owned by Company and are protected by United States and international copyright, trademark, patent, trade secret, and other intellectual property or proprietary rights laws.\n" +
                "\n" +
                "5. RESTRICTIONS\n" +
                "You may not copy, modify, distribute, sell, or transfer any part of the Application without Company's prior written consent. You may not reverse engineer, decompile, or disassemble the Application, or attempt to derive the source code of the Application, except to the extent that such activity is expressly permitted by applicable law.\n" +
                "\n" +
                "6. DISCLAIMER OF WARRANTIES\n" +
                "THE APPLICATION IS PROVIDED \"AS IS\" AND \"AS AVAILABLE\" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. COMPANY DOES NOT WARRANT THAT THE APPLICATION WILL MEET YOUR REQUIREMENTS, OR THAT THE OPERATION OF THE APPLICATION WILL BE UNINTERRUPTED OR ERROR-FREE.\n" +
                "\n" +
                "7. LIMITATION OF LIABILITY\n" +
                "IN NO EVENT SHALL COMPANY BE LIABLE FOR ANY INDIRECT, SPECIAL, INCIDENTAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES ARISING OUT OF OR RELATED TO THE USE OF THE APPLICATION, WHETHER BASED ON CONTRACT, TORT, NEGLIGENCE, STRICT LIABILITY, OR OTHERWISE, EVEN IF COMPANY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.\n" +
                "\n" +
                "8. TERMINATION\n" +
                "This EULA is effective until terminated. Company may terminate this EULA at any time without notice to you. Upon termination, you must immediately cease all use of the Application and destroy all copies of the Application in your possession.\n" +
                "\n" +
                "9. GOVERNING LAW\n" +
                "This EULA shall be governed by and construed in accordance with the laws of the State of [Insert State], without regard to its conflicts of law principles. Any dispute arising out of or related to this EULA shall be subject to the exclusive jurisdiction of the state and federal courts located in [Insert County], [Insert State].\n" +
                "\n" +
                "10. ENTIRE AGREEMENT\n" +
                "This EULA constitutes the entire agreement between you and Company regarding the use of the Application and supersedes all prior agreements and understandings, whether written or oral, regarding the subject matter of this EULA.\n" +
                "\n" +
                "By using the Smart Parking Application, you acknowledge that you have read this EULA, understand it, and agree to be bound by its terms and conditions. If you do not agree to the terms and conditions of this EULA, do not use the Smart Parking Application. ";

        textView.setText(eula);

        //If click I Agree, then toast saying account created, go to login
        //If click I Do Not Agree, then toast saying account not created, go to login
        //Keep track of whether they have agreed before or not, shared preference is probably the way to go, or we can just do it when they register
    }
}