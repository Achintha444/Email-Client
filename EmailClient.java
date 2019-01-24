package emailclient;

// 170638D - T.A.I.Thotawaththa
// In this program there are three external libraries are used
//          * javax.mail.jar
//          * gson-2.2.2.jar
//          * jsoup-1.11.3.jar

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

public class EmailClient {
      public static void main(String[] args) {
          
            System.out.println("EMAIL CLIENT");
            System.out.println("============");
            
            //if your using gmail go to https://www.google.com/settings/security/lesssecureapps and enable
            // any other mail do the same process accordingly
            String yourMail = "achinthaisuru.17@cse.mrt.ac.lk" ; //Senders email here
            String  yourPassword = "anandacollege"; //Senders email password here
            String hostSendEmail = "smtp.gmail.com"; //Host of send Email
            String hostReciveEmail = "pop.gmail.com"; //Host of recieve email
            
            //Reading the clientList.txt 
            BufferedReader br = null;
            ArrayList<Recipient> recipientList = Recipient.create(); //ArrayList containing all the Recipient objects in the text file
            
            //this queue will save the recieved emails
            MyBlockingQueue queue = new MyBlockingQueue(100);
            EmailStatRecoder emailStatRecoder = new EmailStatRecoder();
            EmailStatPrinter emailStatPrinter = new EmailStatPrinter();
            ReciveEmailRemover remover  = new ReciveEmailRemover(queue);
            remover.attach(emailStatPrinter);
            remover.attach(emailStatRecoder);
            Thread threadRemover = new Thread(remover);
            ArrayList<SaveableRecivedMessage> initialRecivedEmails = new ArrayList<>();
            
            try {
            BufferedReader tempBr = new BufferedReader(new FileReader("recivedEmailInformation.txt"));
            String tempMes;
            Gson gson = new Gson();
            SaveableRecivedMessage message ;
            
             while((tempMes = tempBr.readLine())!= null){
                 
                 message = gson.fromJson(tempMes, SaveableRecivedMessage.class);
                 initialRecivedEmails.add(message);
                 
             }
               
        } catch (FileNotFoundException ex) {
            System.out.println("Error with the saved recieved email!\nCHECK AGAIN!!!!!!!!!!!!!!");
        } catch (IOException ex) {
            System.out.println("Error with the saved recieved email!\nCHECK AGAIN!");
        }
                                        
            System.out.println(initialRecivedEmails.size()+" emails already recived!\n");
            RecieveEmail reciveEmail = new RecieveEmail(queue);
            
            Thread recievingEmail = new Thread(){ //This is the thread for reciving email
              @Override
              public void run(){
                  while(true){
                      try{
                      reciveEmail.check(hostReciveEmail, yourMail, yourPassword);
                      }
                      catch(Exception ex){ // if an error occurs this thread will automatically closes it self.
                          System.out.println("Error occured! No new emails will recieve RESTART THE PROGRAM!");
                          stop();
                      }
                  }
                  
              }
            };
            
            recievingEmail.start();
            threadRemover.start();
            
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter option type: \n"
                  + "1 - Adding a new recipient\n"
                  + "2 - Sending an email\n"
                  + "3 - Printing out all the recipients who have birthdays\n"
                  + "4 - Printing out details of all the emails sent\n"
                  + "5 - Printing out the number of recipient objects in the application\n"
                  + "6 - Send Birthday wishes to your recipients!\n"
                  + "7 - Show Recived Emails");
            
            while(true){
            int option = scanner.nextInt();
            
            switch(option){
                
                  case 1:  
                      scanner.nextLine();
                      //input format if
                      // official: <name>, <email>,<designation> 
                      // official friend: <name>, <email>,<designation>,<DOB(yyyy/MM/dd)>
                      //personal friend: <name>,<nick name>, <email>,<DOB(yyyy/MM/dd)>
                      String input = scanner.nextLine();                    
                      try{
                          BufferedWriter bw;
                          bw = new BufferedWriter(new FileWriter("clientList.txt",true));
                          bw.write(input+"\n");
                          bw.close();
                          br = new BufferedReader(new FileReader("clientList.txt"));                                                        
                          System.out.println("New Recipient added successfully!");                          
                      }
                      catch(IOException ex){
                          System.out.println("Input cannot write to the file! \ncheck the file and run again!");
                      }
                      System.out.println("--------------------------------------------------------------------------------");                                                         
                      break;
                      
                  case 2:
                      scanner.nextLine();
                      // input format - email, subject, content
                      String[] temp = scanner.nextLine().split(","); //temp = {email, subject, content}
                      SendEmail.send(temp, yourMail, yourPassword, hostSendEmail); //Send the email to the relevant contact                     
                      System.out.println("--------------------------------------------------------------------------------");
                      break;
                      
                  case 3:
                      scanner.nextLine();
                      // input format - yyyy/MM/dd (ex: 2018/09/17)
                      String dob = scanner.next();
                      int a = 0;
                      System.out.println("Recipients who has birthdays at "+dob+" is/are");
                      for(Recipient r: recipientList){
                          if(r instanceof HasDOB){                     
                              if(dob.equals(((HasDOB) r).getDOB())){
                                  a=1;
                                  r = (Recipient)r;
                                  System.out.println("* "+r.getName()+"\n");
                              }
                          }
                      }
                      if(a==0){
                          System.out.println("Sorry there are no birthdays today!"); 
                      } 
                      System.out.println("--------------------------------------------------------------------------------");                      
                      break;
                      
                  case 4:
                      scanner.nextLine();
                      // input format - yyyy/MM/dd (ex: 2018/09/17)
                      dob = scanner.next();
                       int check= 0;
                        {
                            try {
                                BufferedReader tempBr = new BufferedReader(new FileReader("emailInformation.txt"));
                                String tempMes;
                                Gson gson = new Gson();
                                System.out.println("Details about Emails send on "+dob);
                                System.out.println("==============================");
                                SaveableSentMessage message ;
                                while((tempMes = tempBr.readLine())!= null){
                                    message = gson.fromJson(tempMes, SaveableSentMessage.class);
                                    Date date = message.getDate();
                                    String year = Integer.toString((date.getYear()+1900)); //date.getYear() shows the year whuch minus 1900
                                    String month = String.format("%02d",(date.getMonth()+1)); // +1 must be added
                                    String day = String.format("%02d",date.getDate());
                                    String dateCheck = (year+"/"+month+"/"+day);  
                                    //System.out.println(dateCheck);
                                    if(dob.equals(dateCheck)){
                                        check = 1;
                                        System.out.println("1. Email was sent to "+message.getTo().toString()+"\n"+
                                                             "2. Email's subject was "+message.getSubject()+"\n"+
                                                              "3. Email's content was "+message.getContent().toString()+"\n");
                                    }
                                    
                                }
                            } 
                            catch(EOFException ex){
                                //This catch is implemented because, at first when there is no emailInformation.txt is not created./
                            }                            
                            catch (Exception ex) {                               
                                System.out.println("Relevant file not found!\nPlease Try Again!");
                            }
                        }
                        if(check==0){
                          System.out.println("NONE!"); // Print when no email was sent on that day
                        }
                     
                        System.out.println("--------------------------------------------------------------------------------");
                        break;
                      
                  case 5:
                      // print the number of recipient objects in the application
                      System.out.println("Number of recipient objects in the application is "+Recipient.count);
                      System.out.println("--------------------------------------------------------------------------------");                      
                      break;
                      
                  case 6:
                        //Automatically checks your systems date and send birthday wishes to the listed recipients
                        Date date = new Date();            
                        String month = String.format("%02d",date.getMonth()+1);
                        String day = String.format("%02d",date.getDate());
                        String dateCheck = (month+"/"+day);
                        check = 0;                       
                        for(Recipient r: recipientList){
                            if(r instanceof HasDOB){                                
                                dob = ((HasDOB) r).getDOB().substring(5);                  
                                if(dob.equals(dateCheck)){
                                    check = 1;
                                    temp = new String[3];
                                    r = (Recipient)r;
                                    temp[0] = r.getEmail();
                                    temp[1] = "Happy Birtday!";
                                    if (r instanceof PersonalRecipient){
                                        temp[2] = ("Hugs and love on your birthday!\nFrom Achintha. ");
                                    }
                                    else{
                                        temp[2] = ("Wish you a Happy Birthday.\nFrom Achintha");
                                    }
                                    try{
                                        SendEmail.send(temp, yourMail, yourPassword, hostSendEmail);
                                        System.out.println("Birthday wish sent to "+r.getName());
                                    }
                                    catch(Exception ex){
                                        System.out.println("Birthday wish sent was unsuccessful!\nTry Again!");
                                    }

                                }
                            }
                        }
                        if(check==0){
                            System.out.println("Sorry no birthdays today!");
                        }
                        System.out.println("--------------------------------------------------------------------------------");        
                        break;
                
                case 7:
                      // print the newely recived emails
                      emailStatPrinter.show();                  
                }
            }
      }
}

//-----------------------------------------------------------------------------------------------

interface HasDOB {
    
    public void setDOB(String DOB);
    public String getDOB();
    
}

//-----------------------------------------------------------------------------------------------

abstract class Recipient { 
    
    private String name;
    private String email;
    public static int count;
    
    public Recipient(String name, String email){ //Constructor
        this.name = name;
        this.email = email;
    }
    
    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return this.name;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getEmail(){
        return this.email;
    }
    public static ArrayList<Recipient> create(){
        Recipient recipient = null;
        ArrayList<Recipient> recipientList = new ArrayList<Recipient>();
        try{
            BufferedReader br = new BufferedReader(new FileReader("clientList.txt"));
            String input;
            while((input=br.readLine())!=null){
                    String[] temp = input.split(",");                    
                    if(temp.length==3){
                        recipient = new OfficialRecipient(temp[0],temp[1],temp[2]); // Add a Offical
                        count++;
                    }
                    else if(temp[1].contains("@")){
                        recipient = new OfficialFriendRecipient(temp[0],temp[1],temp[2],temp[3]); //Add a Official Friend
                        count++;
                    }
                    else if(!(temp[1].contains("@"))){
                        recipient = new PersonalRecipient(temp[0],temp[1],temp[2],temp[3]); // Add a personal Friend
                        count++;
                    }
                    else{ // if user has entered a wrong format
                        System.out.println("Wrong Format!");
                    }                      
                    recipientList.add(recipient);
        }
        }
        catch(Exception ex){
           System.out.println("Relevant file not found!\nPlease Try Again!");
        }                
        return recipientList;
}
}

//---------------------------------------------------------------------------------------------------------------

class OfficialRecipient extends Recipient{
    
    private String designation;
    
    public OfficialRecipient(String name,String email, String designation){
        super(name,email);
        this.designation=designation;
    }
    
    public void setDesignation(String designation){
        this.designation = designation;
    }
    public String getDesignation(){
        return this.designation;
    }
}

//---------------------------------------------------------------------------------------------------------------

class OfficialFriendRecipient extends OfficialRecipient implements HasDOB {
    
    private String dob; //date of birth
    
    public OfficialFriendRecipient(String name,String email,String designation,String dob){
        super(name,email,designation);
        this.dob=dob;
    }
    
    @Override
    public void setDOB(String dob){
        this.dob = dob;
    }
    @Override
    public String getDOB(){
        return this.dob;
    }

}
//---------------------------------------------------------------------------------------------------------------

class PersonalRecipient extends Recipient implements HasDOB {
    
    private String nickName;
    private String dob;
    
    public PersonalRecipient(String name,String nickName,String email, String dob){
        super(name,email);
        this.nickName =nickName;
        this.dob = dob;
    }
    
    public void setNickName(String nickName){
        this.nickName = nickName;
    }
    public String getNickName(){
        return this.nickName;
    }
    
    @Override
    public void setDOB(String dob){
        this.dob = dob;
    }
    @Override
    public String getDOB(){
        return this.dob;
    }
}

//---------------------------------------------------------------------------------------------------------------
class SendEmail {
    
    public static void send(String[] temp, String yourMail,String yourPassword,String host){
                    try{
                          boolean sessionDebug = false;
                          
                          Properties props = System.getProperties(); //to set different types of rpoperties
                                                    
                          props.put("mail.smtp.starttls.enable", "true");
                          props.put("mail.smtp.host", host);
                          props.put("mail.smtp.port", "25");
                          props.put("mail.smtp.auth", "true");
                          props.put("mail.smtp.starttls.required", "true");
                          
                          props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");   
                          props.setProperty("mail.smtp.socketFactory.fallback", "false");   
                          props.setProperty("mail.smtp.port", "465");   
                          props.setProperty("mail.smtp.socketFactory.port", "465"); 
                          
                          java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                          
                          Session newSession = Session.getDefaultInstance(props, null);
                          newSession.setDebug(sessionDebug);
                          
                          Message message = new MimeMessage(newSession); //information the email is stored in this object
                          message.setFrom(new InternetAddress(yourMail));
                          InternetAddress address = new InternetAddress(temp[0]);
                          message.setRecipient(Message.RecipientType.TO, address);
                          message.setSubject(temp[1]);
                          Date date = new Date();
                          message.setSentDate(date);
                          message.setText(temp[2]);
                          
                          //Serialize the data about the email using gson-2.2.2.jar library
                          SaveableSentMessage saveMessage = new SaveableSentMessage(temp[0],temp[1],temp[2],date);                          
                          Gson gson = new Gson();
                          try (BufferedWriter bw = new BufferedWriter(new FileWriter("emailInformation.txt",true))) {
                            bw.write(gson.toJson(saveMessage)+"\n");
                           }
                  
                          Transport transport = newSession.getTransport("smtps");//Use this to send the email
                          transport.connect(host, yourMail, yourPassword);
                          transport.sendMessage(message, message.getAllRecipients());
                          transport.close();
                          System.out.println("Email Successfully Sent!");
                      }
                      catch(Exception ex){                          
                          System.out.println("Error occured while sending the email!\nTry Again!"+ex);
                      }
    }
}
//---------------------------------------------------------------------------------------------------------------
class SaveableSentMessage { // This class is used to save data about sent emails
    
    private String to;
    private String subject;
    private String content;
    private Date date;
    
    public SaveableSentMessage(String to, String subject, String content, Date date) {
        this.to=to;
        this.subject = subject;
        this.content = content;
        this.date = date;
    }
    
    public void setTo(String to){
        this.to = to;
    }
    public String getTo(){
        return this.to;
    }
     public void setSubject(String subject){
        this.subject= subject;
    }
    public String getSubject(){
        return this.subject;
    }
     public void setContent(String Content){
        this.to = content;
    }
    public String getContent(){
        return this.content;
    }
     public void setDate(Date date){
        this.date = date;
    }
    public Date getDate(){
        return this.date;
    }
}
//---------------------------------------------------------------------------------------------------------------
class SaveableRecivedMessage{
    
    private Date recivedDate;
    private String from;
    private String content;
    private String subject;

    public SaveableRecivedMessage(Date recivedDate, String from, String content, String subject) {
        this.recivedDate = recivedDate;
        this.from = from;
        this.content = content;
        this.subject = subject;
    }

    public Date getRecivedDate() {
        return this.recivedDate;
    }

    public void setRecivedDate(Date recivedDate) {
        this.recivedDate = recivedDate;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }    
}

//---------------------------------------------------------------------------------------------------------------

interface Observable { //Custom Observable interface
    
    public void attach(Observer observer);
    public void notifyAllObservers(SaveableRecivedMessage message);   
}

//---------------------------------------------------------------------------------------------------------------

interface Observer {//Custom Observer interface
    
    public void update(SaveableRecivedMessage message);
    
}

//---------------------------------------------------------------------------------------------------------------

class MyBlockingQueue { //This queue has a maximum size of 100
    
    private LinkedList<SaveableRecivedMessage> myQueue;
    private int maxSize;
    
    public MyBlockingQueue(int maxSize){
        this.myQueue = new LinkedList<>();
        this.maxSize = maxSize;
    }
    
    public synchronized void enqueue(SaveableRecivedMessage message){
        int length = myQueue.size();
        while(length == this.maxSize){
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyBlockingQueue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.myQueue.add(message);
        notifyAll(); //to notify that an item is added to the qeueue
    }
    
    public synchronized SaveableRecivedMessage dequeue(){        
        while(myQueue.isEmpty()){            
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }       
        SaveableRecivedMessage temp = myQueue.remove();        
        notifyAll();
        return temp;
        
    }

}

//---------------------------------------------------------------------------------------------------------------

 class RecieveEmail {
    
    private MyBlockingQueue queue;
    private Observer[] observers;

    public RecieveEmail(MyBlockingQueue queue) {
        this.queue = queue;
        this.observers = new Observer[2];
    }
    
   public  void check(String host, String user, String password) 
   {
      try {
      Properties properties = new Properties();
      properties.put("mail.pop3.host", host);
      properties.put("mail.pop3.port", "995");
      properties.put("mail.pop3.starttls.enable","true");
      Session emailSession = Session.getDefaultInstance(properties);
  
      //create the POP3 store object and connect with the pop server
      Store store = emailSession.getStore("pop3s");
      store.connect(host, user, password);

      //create the folder object and open it
      Folder emailFolder = store.getFolder("INBOX");
      emailFolder.open(Folder.READ_ONLY);

      // retrieve the messages from the folder in an array and print it
       Message messages[] = emailFolder.search(new FlagTerm(new Flags(
                    Flags.Flag.SEEN), false));
      ArrayList<SaveableRecivedMessage> tempMessages = new ArrayList<>();
      
      for (int i = 0, n = messages.length; i < n; i++) {
         Message message = messages[i];
         Date date = message.getSentDate();
         String from = message.getFrom()[0].toString();
         String content = getTextFromMessage(message);
         String subject = message.getSubject();
         SaveableRecivedMessage temp = new SaveableRecivedMessage(date,from,content,subject);
         message.setFlag(Flags.Flag.SEEN, true);
              
         this.queue.enqueue(temp);
         tempMessages.add(temp);
   //      }
      }
      
      if (!tempMessages.isEmpty()) {
          //notifyAllObservers(tempMessages,tempMessages.size());
          System.out.println(tempMessages.size()+" -- Email/Emails Recived");
          tempMessages = new ArrayList<>();
      }

      //close the store and folder objects
      emailFolder.close(false);
      store.close();

      } catch (NoSuchProviderException e) {
         System.out.println("Email reciving error!");
      } catch (MessagingException e) {
         System.out.println("Email reciving error!");
      } catch (Exception e) {
         System.out.println("Email reciving error!");
      }
   }
   
   private  String getTextFromMessage(Message message) throws MessagingException, IOException {
    String result = "";
    if (message.isMimeType("text/plain")) {
        result = message.getContent().toString();
    } else if (message.isMimeType("multipart/*")) {
        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
        result = getTextFromMimeMultipart(mimeMultipart);
    }
    return result;
}

private  String getTextFromMimeMultipart(
    MimeMultipart mimeMultipart)  throws MessagingException, IOException{
    String result = "";
    int count = mimeMultipart.getCount();
    for (int i = 0; i < count; i++) {
        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
        if (bodyPart.isMimeType("text/plain")) {
            result = result + "\n" + bodyPart.getContent();
            break; // without break same text appears twice in my tests
        } else if (bodyPart.isMimeType("text/html")) {
            String html = (String) bodyPart.getContent();
            result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
        } else if (bodyPart.getContent() instanceof MimeMultipart){
            result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
        }
    }
    return result;
}

}

//---------------------------------------------------------------------------------------------------------------

class ReciveEmailRemover implements Observable , Runnable{ //This class will remove the message object from the MyBlockingQueue
    
    private MyBlockingQueue queue;
    private Observer[] observers;
    private int counter;

    public ReciveEmailRemover(MyBlockingQueue queue) {
        this.queue = queue;
        this.observers = new Observer[2];
        this.counter = 0;
    }
    
    @Override
    public void run(){
        while(true){
        SaveableRecivedMessage temp = this.queue.dequeue();
        notifyAllObservers(temp);
        }
    }

    @Override
    public void attach(Observer observer) {
       this.observers[this.counter] = observer;
       this.counter++;
        
    }

    @Override
    public void notifyAllObservers(SaveableRecivedMessage message) {
        for (int i = 0; i<2 ; i++){
            this.observers[i].update(message);
        }
    }   
}

//---------------------------------------------------------------------------------------------------------------

class EmailStatRecoder implements Observer{ //This will save the email message records in a text document
    
    private ArrayList<SaveableRecivedMessage> messages;
    private static SaveableRecivedMessage temp;
    
    

    public EmailStatRecoder() {
        messages = new ArrayList<>();
    }
    
    public ArrayList<SaveableRecivedMessage> Initialrecoder(){
        try {
            BufferedReader tempBr = new BufferedReader(new FileReader("recivedEmailInformation.txt"));
            String tempMes;
            Gson gson = new Gson();
            SaveableRecivedMessage message ;
            
             while((tempMes = tempBr.readLine())!= null){
                 
                 message = gson.fromJson(tempMes, SaveableRecivedMessage.class);
                 this.messages.add(message);
                 
             }
               
        } catch (FileNotFoundException ex) {
            System.out.println("Error with the saved recieved email!\nCHECK AGAIN!!!!!!!!!!!!!!");
        } catch (IOException ex) {
            System.out.println("Error with the saved recieved email!\nCHECK AGAIN!");
        }
        
        return this.messages;
    }
    
    @Override
    public void update(SaveableRecivedMessage message){
        temp = message;
        saveToFile();

    }
    
    public static void saveToFile(){
        Gson gson = new Gson();
        try { 
            
              BufferedWriter bw = new BufferedWriter(new FileWriter("recivedEmailInformation.txt",true));
              bw.write(gson.toJson(temp)+"\n");
              bw.close();

            } catch (IOException ex) {
                System.out.println("Error occured when saving the recived email!");
            }              
    }

}

//---------------------------------------------------------------------------------------------------------------

class EmailStatPrinter implements Observer{ //This class will print the recently recived emails in the console

    private ArrayList<SaveableRecivedMessage> tempList;
  
    public EmailStatPrinter() {

        this.tempList = new ArrayList<>();
    }
    
    @Override
    public void update(SaveableRecivedMessage message) { // print newly recived emails to the console
        this.tempList.add(message);

    }
    
    public void show(){       
        for (int i =0; i<this.tempList.size() ; i++){
           SaveableRecivedMessage temp = this.tempList.get(i);
           Date recivedDate = temp.getRecivedDate();
            System.out.println("From: "+temp.getFrom()+"\n");
            System.out.println("Recived Date: "+recivedDate.toString()+"\n");
            System.out.println("Subject: "+temp.getSubject()+"\n");
            System.out.println("Content: \n"+temp.getContent());            
            System.out.println("------------------------------------------");      
        }
        if (this.tempList.isEmpty()) System.out.println("There are no new emails recieved!");
        else this.tempList = new ArrayList<>();
    }   
}

//---------------------------------------------------------------------------------------------------------------