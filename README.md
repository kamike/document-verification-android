# Trulioo Sample App with Acuant Android SDK v11.4.0

**Disclaimer**
This sample application is a prototype written to demonstrate how to integrate with the Trulioo platform. It is by no means production ready and will not be supported by Trulioo. It is being hosted here strictly as an example and should not be used as a foundation for your Document Capture Applications.

You should have received an integration guide from Trulioo to show how to try this demo application.

# Quick Start
Enter Acuant account in  app/src/main/assets/ acuant.config.xml
Enter Trulioo credentials in  app/src/main/java/com/acuant/sampleapp/TruliooVerificationHelper.kt
  
**As of April 24, 2020, the acuant.config.xml file should look like this**  
<?xml version="1.0" encoding="UTF-8" ?>  
<setting>  
    <acuant_username>XXX</acuant_username>  
    <acuant_password>XXX</acuant_password>  
    <acuant_subscription></acuant_subscription>  
    <ozone_subscription></ozone_subscription>  
    <med_endpoint>https://medicscan.acuant.net</med_endpoint>  
    <assureid_endpoint>https://services.assureid.net</assureid_endpoint>  
    <passive_liveness_endpoint>https://passlive.acuant.net</passive_liveness_endpoint>  
    <acas_endpoint>https://acas.acuant.net</acas_endpoint>  
    <ozone_endpoint>https://ozone.acuant.net</ozone_endpoint>  
</setting>  