package com.parking.linkandpark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class eulaActivity extends AppCompatActivity {

    /**
     * onCreate function to instantiate the textfield with our end user license agreement
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        // Set field equal to the user terms text
        TextView textView = findViewById(R.id.eulaText);
        String eula = "END USER LICENSE AGREEMENT (EULA) FOR SMART PARKING APPLICATION\n" +
                "\n" +
                "PLEASE READ THIS END USER LICENSE AGREEMENT (\"EULA\") CAREFULLY BEFORE USING THE SMART PARKING APPLICATION: LINK & PARK.\n" +
                "\n" +
                "1. GENERAL TERMS\n" +
                "This EULA is a legal agreement between you, as either an individual or a single entity, and [MNJ Software Solutions]" +
                " (hereafter referred to as \"Company\"), the developer of the Link & Park " +
                "(hereafter referred to as the \"Application\"). By using the Application, you agree to be bound by the terms of this" +
                " EULA. If you do not agree to the terms of this EULA, do not use the Application.\n" +
                "\n" +
                "2. LICENSE GRANT\n" +
                "Subject to the terms and conditions of this EULA, the Company hereby grants you a limited, non-exclusive," +
                " non-transferable, revocable license to use the Application solely for the purpose of locating available " +
                " spots on a university campus. This license is for personal and non-commercial use only.\n" +
                "\n" +
                "3. USER DATA\n" +
                "The Application may use your phone's location to determine available parking spots on a university campus. By" +
                " using the Application, you agree to the collection, transmission, processing, and storage of your location data" +
                " by the Application. The Company will use your data solely for the purpose of providing the services of the Application" +
                " and will not share your data with third parties without your consent, except as required by law.\n" +
                "\n" +
                "4. PROPRIETARY RIGHTS\n" +
                "The Application is owned and operated by the Company. The Application and its entire contents, features, and functionality" +
                " (including but not limited to all information, software, text, displays, images, video, and audio) are owned by the Company" +
                " and are protected by Canadian and international copyright, trademark, patent, trade secret, and other intellectual property" +
                " or proprietary rights laws. You acknowledge and agree that the Application and its contents are the property of the Company," +
                " and that you have no right to use the Application or its contents in any manner other than as expressly permitted under this" +
                " EULA.\n" +
                "\n" +
                "5. RESTRICTIONS\n" +
                "You may not copy, modify, distribute, sell, or transfer any part of the Application without the Company's prior written consent." +
                " You may not reverse engineer, decompile, or disassemble the Application, or attempt to derive the source code of the Application," +
                " except to the extent that such activity is expressly permitted by applicable law. You may not use the Application in any way that" +
                " violates any applicable laws or regulations.\n" +
                "\n" +
                "6. DISCLAIMER OF WARRANTIES\n" +
                "THE APPLICATION IS PROVIDED \"AS IS\" AND \"AS AVAILABLE\" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT" +
                " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. COMPANY DOES NOT WARRANT THAT" +
                " THE APPLICATION WILL MEET YOUR REQUIREMENTS, OR THAT THE OPERATION OF THE APPLICATION WILL BE UNINTERRUPTED OR ERROR-FREE. YOU ASSUME ALL" +
                " RESPONSIBILITY FOR THE SELECTION OF THE APPLICATION TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE INSTALLATION, USE, AND RESULTS OBTAINED FROM" +
                " THE APPLICATION.\n" +
                "\n" +
                "7. LIMITATION OF LIABILITY\n" +
                "IN NO EVENT SHALL THE COMPANY BE LIABLE FOR ANY INDIRECT, SPECIAL, INCIDENTAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES ARISING OUT OF OR RELATED TO THE" +
                " USE OF THE APPLICATION, WHETHER BASED ON CONTRACT, TORT, NEGLIGENCE, STRICT LIABILITY, OR OTHERWISE, EVEN IF THE COMPANY HAS BEEN ADVISED OF THE" +
                " POSSIBILITY OF SUCH DAMAGES. THE COMPANY'S LIABILITY SHALL NOT EXCEED THE AMOUNT YOU PAID FOR THE APPLICATION, IF ANY.\n" +
                "\n" +
                "8. TERMINATION\n" +
                "This EULA will remain effective until terminated by either party. You may terminate this EULA at any time by uninstalling the Application and" +
                " destroying all copies of the Application in your possession. Company may also terminate this EULA at any time without notice to you if you" +
                " breach any of its terms or conditions. Upon termination, you must immediately cease all use of the Application and destroy all copies of the" +
                " Application in your possession.\n" +
                "\n" +
                "9. GOVERNING LAW\n" +
                "This EULA shall be governed by and construed in accordance with the laws of the Province of Ontario, Canada, without regard to its conflicts of" +
                " law principles. Any dispute arising out of or related to this EULA shall be subject to the exclusive jurisdiction of the courts of the Province of" +
                " Ontario, Canada.\n" +
                "\n" +
                "10. ENTIRE AGREEMENT\n" +
                "This EULA constitutes the entire agreement between you and Company regarding the use of the Application and supersedes all prior agreements and" +
                " understandings, whether written or oral, regarding the subject matter of this EULA. Any waiver, modification, or amendment of any provision of this" +
                " EULA will be effective only if in writing and signed by Company.\n" +
                "\n" +
                "11. SEVERABILITY\n" +
                "If any provision of this EULA is found to be invalid or unenforceable, the remaining provisions shall remain in full force and effect. Any such" +
                " invalid or unenforceable provision shall be replaced with a valid and enforceable provision that most closely reflects the original intent of the" +
                " parties.\n" +
                "\n" +
                "12. AMENDMENTS\n" +
                "Company reserves the right, at its sole discretion, to modify or replace this EULA at any time. If a revision is material, we will provide at least" +
                " 30 days' notice prior to any new terms taking effect. What constitutes a material change will be determined at our sole discretion.\n" +
                "\n" +
                "13. ASSIGNMENT\n" +
                "You may not assign or transfer this EULA, by operation of law or otherwise, without Company's prior written consent. Any attempt by you to assign or" +
                " transfer this EULA, without such consent, will be null and of no effect. Company may assign or transfer this EULA, in whole or in part, to any third" +
                " party without restriction.\n" +
                "\n" +
                "14. SURVIVAL\n" +
                "The provisions of Sections 4, 5, 6, 7, 8, 9, 10, 11, 13, and 14 shall survive the termination of this EULA.\n" +
                "\n" +
                "15. CONTACT INFORMATION\n" +
                "If you have any questions about this EULA, please contact us at jtsang@lakeheadu.ca.\n" +
                "\n" +
                "By using the Smart Parking Application, you acknowledge that you have read this EULA, understand it, and agree to be bound by its terms and conditions." +
                " If you do not agree to the terms and conditions of this EULA, do not use the Application.\n";
        textView.setText(eula);

        // If the user accepts terms and conditions, navigate them to the registration screen and display popup
        Button acceptButton = findViewById(R.id.agreeButton);
        acceptButton.setOnClickListener(view -> {
            Intent intent = new Intent(eulaActivity.this, RegisterActivity.class);
            startActivity((intent));
            finish();
        });

        // If the user does not accept terms and conditions, navigate them back to login screen and display popup
        Button doNotAcceptButton = findViewById(R.id.doNotAgreeButton);
        doNotAcceptButton.setOnClickListener(view -> {
            Intent intent = new Intent(eulaActivity.this, LoginActivity.class);
            Toast.makeText(getApplicationContext(), "Must accept terms in order to register.", Toast.LENGTH_SHORT).show();
            startActivity((intent));
            finish();
        });

    }
}