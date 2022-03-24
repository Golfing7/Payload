# Payload
*Fail-safe asynchronous profile caching via Redis &amp; MongoDB in Java for Spigot. Created originally by jonahseguin who is now a big Hypixel developer man... Updated to 1.17 & & Java  16 by Savag3life*

Yes, this is a very long readme - GitHub wants us to pay like $160 USD a year to have a wiki on a private repo :kekw:

## Payload Types
Payload offers two types of Payloads. `PayloadObject` & `PayloadProfile`. Both types require 2 parts - A "Data Object" which is used as an in-memory interface with the backing database. It also requires a loader/manager class to maintain to reference the database itself.

### PayloadObjects
These "objects" are standalone and bound to a `ObjectID` when created. They are used to store data about NON-PROFILE data such as Pickaxe, Gangs, etc.

#### Usage
Object Class
```java
@Entity("Mine")
public class Mine extends PayloadObject {
    // The @Entity annotation IS REQUIRED - This is the name of the Collection in MongoDB

    // This field is REQUIRED - This is the **unique** ID of the object
    // Payload will also create a ObjectID for each object to prevent duplicates
    // But there should only be one object, with one identifier loaded at a time.
    @Getter @Setter private String identifier;
    
    // Now you add any fields you'd like stored in the database
    @Getter @Setter private MineStatus status = MineStatus.INVITE_ONLY;
    @Getter @Setter private Material mineBlock;
    @Getter @Setter private boolean premiumMine = false;
    @Getter @Setter private double taxRate = 0.0D;

    @NotNull @Override
    public String identifierFieldName() {
        return "identifier"; // This is the plan text name for the "identifier" field above.
    }

    // Required
    public Mine(ObjectCache cache) {
        super(cache);
    }

    // Required
    public Mine() {
        // This would be an instance of your parent manager / controller class.
        super(Mines.getInstance());
    }
}
```
Now you'd also need a Controller / Manager class to handle the database.
```java
// The PayloadCache should have a reference to your data object. In this case, Mine.java
public class Mines extends PayloadObjectCache<Mine> {

    @Getter private static Mines instance;

    public Mines() {
        super(
                Core.getInstance().getInjector(),     // Injector from Payload "Module"
                injector -> new Mine(Mines.instance), // How to create a new PrisonPlayer
                "Mine",                               // Cache name
                Mine.class                            // Class of the cache
        );
        instance = this; // Assign the instance to itself, so we can use Mines.getInstance()
        if (!this.database.isConnected()) this.database.start(); // Check if the backing database is connected
        start(); // Start the cache 
        getApi().saveCache(this); // Make sure the backing API is aware of the cache
    }
}
```
Now to create a new Mine, you'd do something like this:
```java
public class MineCreation {
    public Mine createNewMine() {
        Mine mine = Mines.getInstance().create(); // Create a new Mine object using Payload & Guice
        // Do all your mine setup logic here. Like setting regions, spawn points, default values etc
        Mines.getInstance().cache(mine); // Make sure to cache the mine after creating, or it'll never be found again.
    }
}
```
If we want to retrieve a mine, we'd do something like this:
```java
public class MineRetrieval {
    public Mine getMine(String identifier) {
        Optional<Mine> mine = Mines.getInstance().get(identifier); // Get a mine by its identifier 
        // The result is optional because the identifier is not controlled by a static factor like a players UUID
        // So you'll need to ensure that the result is actually there before attempting to use.
        if (mine.isPresent()) {
            // Do something with the mine
        }
        return mine;
    }
}
```

### PayloadProfiles
Payload Profiles are bound directly to a player. Data is loaded when the player logs into the server, and unloaded automatically when the player logs out after a period of inactivity.

#### Usage
Profile Class
```java
@Entity("PrisonPlayer")
@NoArgsConstructor
public class PrisonPlayer extends PayloadProfile {
    // The @Entity annotation IS REQUIRED - This is the name of the Collection in MongoDB

    // You don't need to create an identifier field, Payload will assign the players UUID as the key & use it to load the player automatically.
    
    // Now you add any fields you'd like stored in the database
    @Getter @Setter private int level = 1;

    // First Load Method / Created - new PrisonsPlayer();
    public PrisonPlayer(PrisonPlayers cache) {
        super(cache);
    }

    // Loaded from DB - PrisonsPlayers.get();
    @Override
    protected void init() {

    }

    // Saved to DB - PrisonsPlayers.save();
    @Override
    protected void uninit() {

    }
}
```
Now you'd also need a Controller / Manager class to handle the database.
```java
public class PrisonPlayers extends PayloadProfileCache<PrisonPlayer> {

    @Getter private static PrisonPlayers instance;

    public PrisonPlayers() {
        super(
                Core.getInstance().getInjector(),                     // Injector from Payload "Module"
                injector -> new PrisonPlayer(PrisonPlayers.instance), // How to create a new PrisonPlayer
                "PrisonPlayers",                                      // Cache name
                PrisonPlayer.class                                    // Class of the cache
        );

        instance = this; // Assign the instance to itself, so we can use PrisonPlayers.getInstance()
        if (!this.database.isConnected()) this.database.start(); // Check if the backing database is connected
        start(); // Start the cache 
        getApi().saveCache(this); // Make sure the backing API is aware of the cache
    }
}
```
You don't need to create new instancing of Profile objects, Payload will automatically create them for you.
If you wanted to retrieve a PrisonPlayer, you'd do something like this:
```java
public class PrisonPlayerRetrieval {
    public PrisonPlayer getPrisonPlayer(String identifier) {
        Optional<PrisonPlayer> player = PrisonPlayers.getInstance().get(identifier); // Get a PrisonPlayer by its identifier
        // The result is optional as the player may have not logged in or the data was removed by another server (instancing)
        // So you'll need to ensure that the result is actually there before attempting to use.
        if (player.isPresent()) {
            // Do something with the player
        }
        
        return player;
    }
}
```

### Parent / Manager Class
Payload uses Guice for dependency injection. By doing this is also requires additional setup in your main plugin / main class. In most of our use cases, there will be a parent setup in the core plugin for the realm we are working in. If not, you'll need to add one.

## Creating a Payload Module
```java
public class MyModule extends JavaPlugin {

    @Getter private PayloadModule databaseModule; // This will be OUR module
    @Getter private Injector injector; // The Guice injector instance - You'll need this for nothing except Payload.
    
    @Override
    public void onEnable() {
        super.onEnable();
        // Ask Payload to create a new extension module for us - The name must be unique.
        this.databaseModule = PayloadAPI.install(this, "prisons");
        // Create an extension Guice Injector
        // This is the Injector passed to constructors of our Payload Objects/Profiles to create them 
        // at runtime. This allows us to inject our own dependencies into the Payload objects.
        this.injector = Guice.createInjector(this.databaseModule);
    }
}
```
Make sure you also add Payload to your dependencies in your plugin.yml - If not it will be very mad at you when it goes to load your plugin.

### Setup & Databases
For MineQuest / Buzz - We run in a multi-node private network environment. This mean all our databases are run on a central MongoDB cluster. You will almost always use the following credentials to connect to mongo and redis.
```yaml
mongodb:
  uri: null
  address: '10.8.1.1'
  port: 27017 
  database: 'prisons' 
  auth:
    enabled: false
    authDatabase: 'admin'
    username: 'username'
    password: 'password'

redis:
  uri: 'redis://thisshouldntbeneeded@minequest.jamo.tech:6379/1'
  address: ''
  port: 1
  auth:
    ssl: false
    enabled: false
    password: '''
```
Keep in mind, in a multi-server environment, with each server having its own set of collections & databases you'll need to have each Redis cache use a unique database number. To do this, you simply append a `/1` to the end of your redis URI. Redis has upto 16 (base0) unique databases, so you can use `/1` through `/15` to use different databases. A non-spefiied database will default to `/0`. 