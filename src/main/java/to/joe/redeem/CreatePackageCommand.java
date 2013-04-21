package to.joe.redeem;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.BooleanPrompt;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.PluginNameConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CreatePackageCommand implements CommandExecutor {//TODO Custom coupon codes TODO Allow review before inserting

    private ConversationFactory factory;
    private RedeemMe plugin;

    public CreatePackageCommand(RedeemMe plugin) {
        this.plugin = plugin;
        this.factory = new ConversationFactory(this.plugin).withEscapeSequence("abort").thatExcludesNonPlayersWithMessage(ChatColor.RED + "Only players may use this command").withFirstPrompt(new NamePrompt()).withModality(false).withPrefix(new PluginNameConversationPrefix(this.plugin, "> ", ChatColor.GOLD));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable) {
            sender.sendMessage(ChatColor.GREEN + "Say \"abort\" at any time to cancel package creation.");
            factory.buildConversation((Conversable) sender).begin();
        }
        return true;
    }

    private class NamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            context.setSessionData("builder", new PackageBuilder());
            return ChatColor.GREEN + "What should this package be named? (Enter \"none\" for no name)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (!input.equalsIgnoreCase("none")) {
                ((PackageBuilder) context.getSessionData("builder")).withName(input);
            }
            return new DescriptionPrompt();
        }
    }

    private class DescriptionPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "What should the description of this package be? (Enter \"none\" for no description)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (!input.equalsIgnoreCase("none")) {
                ((PackageBuilder) context.getSessionData("builder")).withDescription(input);
            }
            return new CreatorPrompt();
        }
    }

    private class CreatorPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Who or what should the creator of this package be? (Enter \"none\" for no creator)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (!input.equalsIgnoreCase("none")) {
                ((PackageBuilder) context.getSessionData("builder")).withCreator(input);
            }
            return new MoneyPrompt();
        }
    }

    private class MoneyPrompt extends NumericPrompt {//TODO Needs perms

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "How much money should this package have? (Enter 0 for none)";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.doubleValue() >= 0;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "Number must be greater than or equal to zero";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
            if (input.doubleValue() > 0) {
                ((PackageBuilder) context.getSessionData("builder")).withMoney(input.doubleValue());
            }
            return new PreItemPrompt();
        }
    }

    private class PreItemPrompt extends MessagePrompt {//TODO Needs perms

        @Override
        public String getPromptText(ConversationContext context) {
            if (context.getForWhom() instanceof Player) {
                Inventory inv = plugin.getServer().createInventory(null, 27, "Package Items");
                context.setSessionData("inventory", inv);
            }
            return ChatColor.GREEN + "Select items you wish to include in the package.";
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new ItemPrompt();
        }
    }

    private class ItemPrompt implements Prompt {

        @Override
        public String getPromptText(ConversationContext context) {
            Player player = (Player) context.getForWhom();
            player.openInventory((Inventory) context.getSessionData("inventory"));
            return ChatColor.GREEN + "Place the items you wish to include into the top half of the inventory screen. Type \"done\" when you are finished. Type anything else to show this message and open the inventory again. You are not requried to add any items.";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("done")) {
                Inventory inv = (Inventory) context.getSessionData("inventory");
                PackageBuilder builder = ((PackageBuilder) context.getSessionData("builder"));
                for (ItemStack item : inv.getContents()) {
                    if (item != null) {
                        builder.withItemStack(item);
                        ((Player) context.getForWhom()).getInventory().addItem(item);
                    }
                }
                return new CommandPrompt();
            } else {
                return new ItemPrompt();
            }
        }

        @Override
        public boolean blocksForInput(ConversationContext context) {
            return true;
        }
    }

    private class CommandPrompt extends StringPrompt {//TODO This needs perms! Otherwise an admin will be able to create a command that gives them more power!

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Enter a command you wish this package to run. The string <player> will be replaced with the name of the player redeeming the package. Say \"done\" when you are finished. You are not required to enter any commands.";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("done")) {
                return new EmbargoPrompt();
            } else {
                context.setSessionData("command", input);
                return new ConsolePrompt();
            }
        }
    }

    private class ConsolePrompt extends BooleanPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Should the command \"" + context.getSessionData("command") + "\" be run as console?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            ((PackageBuilder) context.getSessionData("builder")).withCommand((String) context.getSessionData("command"), input);
            return new CommandPrompt();
        }
    }

    private class EmbargoPrompt extends ValidatingPrompt {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "What should the embargo be set to for this package? Respond in the format yyyy-MM-dd HH:mm:ss. Say \"none\" for no embargo. The current time is " + sdf.format(new Date());
        }

        @Override
        protected boolean isInputValid(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("none")) {
                return true;
            }
            try {
                Date date = sdf.parse(input);
                if (System.currentTimeMillis() > date.getTime()) {
                    return false;
                }
                ((PackageBuilder) context.getSessionData("builder")).withEmbargo(date.getTime() / 1000);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return "Invalid date format or embargo has already occured.";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            return new ExpiryPrompt();
        }
    }

    private class ExpiryPrompt extends ValidatingPrompt {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "What should the expiry be set to for this package? Respond in the format yyyy-MM-dd HH:mm:ss. Say \"none\" for no expiry.  The current time is " + sdf.format(new Date());
        }

        @Override
        protected boolean isInputValid(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("none")) {
                return true;
            }
            try {
                Date date = sdf.parse(input);
                if (System.currentTimeMillis() > date.getTime()) {
                    return false;
                }
                ((PackageBuilder) context.getSessionData("builder")).withExpiry(date.getTime() / 1000);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return "Invalid date format or expiry has already occured.";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            return new ServerPrompt();
        }
    }

    private class ServerPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Which server should this package be valid on? (Enter * for all servers)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (!input.equals("*")) {
                ((PackageBuilder) context.getSessionData("builder")).onServer(input);
            }
            return new CouponPlayerPrompt();
        }
    }

    private class CouponPlayerPrompt extends FixedSetPrompt {

        public CouponPlayerPrompt() {
            super("player", "coupon");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Should this package be for a player only, or should it have a coupon code?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("player")) {
                return new PlayerPrompt();
            }
            if (input.equalsIgnoreCase("coupon")) {
                return new CouponRemainingPrompt();
            }
            return null;
        }
    }

    private class PlayerPrompt extends RegexPrompt {

        public PlayerPrompt() {
            super("[A-Za-z0-9_]{2,16}");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "Which player should be able to redeem this package?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            try {
                ((PackageBuilder) context.getSessionData("builder")).forPlayer(input).build();
                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Sucessfully created package!");
            } catch (SQLException e) {
                context.getForWhom().sendRawMessage(ChatColor.RED + "There was a SQL error when creating the package.");
                plugin.getLogger().log(Level.SEVERE, "Error creating package", e);
            }
            return Prompt.END_OF_CONVERSATION;
        }
    }

    private class CouponRemainingPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GREEN + "How many coupons should be available? (Enter -1 for unlimited)";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() == -1 || input.intValue() > 0;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "Number must be -1 or greater than 0";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
            try {
                CouponCode code = new CouponCode(input.intValue());
                ((PackageBuilder) context.getSessionData("builder")).withCode(code).build();
                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Sucessfully created coupon with code \"" + code.getCode() + "\"");
            } catch (SQLException e) {
                context.getForWhom().sendRawMessage(ChatColor.RED + "There was a SQL error when creating the package.");
                plugin.getLogger().log(Level.SEVERE, "Error creating coupon", e);
            }
            return Prompt.END_OF_CONVERSATION;
        }
    }
}