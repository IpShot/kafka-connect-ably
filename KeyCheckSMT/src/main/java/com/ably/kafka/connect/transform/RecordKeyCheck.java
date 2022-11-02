package com.ably.kafka.connect.transform;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.nio.charset.StandardCharsets;
import java.util.Map;


public class RecordKeyCheck<R extends ConnectRecord<R>> implements Transformation<R> {
    private final static String KEY_TOKEN = "#{key}";
    private final static String CHANNEL_CONFIG = "channel.name";
    private final static String MESSAGE_CONFIG = "message.name";
    public static final ConfigDef CONFIG_DEF;
    private String channelConfig;
    private String messageNameConfig;

    @Override
    public R apply(R record) {
        final byte[] key = (byte[]) record.key();
        String keyString = null;
        if (key != null && ByteArrayUtils.isUTF8Encoded(key)) {
            keyString = new String(key, StandardCharsets.UTF_8);
        }

        if (keyString == null && (channelConfig.contains(KEY_TOKEN) || messageNameConfig.contains(KEY_TOKEN))) {
            System.out.println(this.getClass().getSimpleName()+": Key is null or not a string type but pattern contains #{key}");
            throw new IllegalArgumentException("Key is null or not a string type but pattern contains #{key}");
            //This SMT shouldn't be set if skippable is true - so we can throw an exception here
        }
        return record;
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> map) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, map);
        if (config.getString(CHANNEL_CONFIG)  == null) {
            throw new ConfigException("You must provide channel.name for this SMT");
        }
        //it's useless if the channel name doesn't contain the key token
        if (!config.getString(CHANNEL_CONFIG).contains(KEY_TOKEN)) {
            throw new ConfigException("Channel name must contain #{key} token");
        }

        if (config.getString(MESSAGE_CONFIG)  != null && !config.getString(MESSAGE_CONFIG).contains(KEY_TOKEN)) {
            throw new ConfigException("Message name must contain #{key} token");
        }

        this.channelConfig = config.getString(CHANNEL_CONFIG);
        this.messageNameConfig = config.getString(MESSAGE_CONFIG);
    }

    static {
        CONFIG_DEF = new ConfigDef().
            define(CHANNEL_CONFIG,
                ConfigDef.Type.STRING,
                null,
                ConfigDef.Importance.HIGH,
                "The channel name to publish to")
            .define(MESSAGE_CONFIG,
                ConfigDef.Type.STRING,
                null,
                ConfigDef.Importance.LOW,
                "The message name to publish");
    }
}
