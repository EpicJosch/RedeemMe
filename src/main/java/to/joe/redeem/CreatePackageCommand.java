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

import to.joe.redeem.exception.CouponCodeAlreadyExistsException;
import to.joe.redeem.exception.IncompletePackageException;

public class CreatePackageCommand implements CommandExecutor {

    private ConversationFactory factory;
    private RedeemMe plugin;

    public CreatePackageCommand(RedeemMe plugin) {
        this.plugin = plugin;
        this.factory = new ConversationFactory(this.plugin).withEscapeSequence("abort").thatExcludesNonPlayersWithMessage("Only players may use this command").withFirstPrompt(new NamePrompt()).withModality(false).withPrefix(new PluginNameConversationPrefix(this.plugin, "> ", ChatColor.GOLD));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable) {
            sender.sendMessage(ChatColor.GOLD + "RedeemMe> " + ChatColor.BLUE + "Say " + ChatColor.RED + "abort" + ChatColor.BLUE + " at any time to " + ChatColor.GOLD + "cancel " + ChatColor.BLUE + "package creation.");
            factory.buildConversation((Conversable) sender).begin();
        }
        return true;
    }

    private class NamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            context.setSessionData("builder", new PackageBuilder());
            return ChatColor.BLUE + "What should the " + ChatColor.GOLD + "name " + ChatColor.BLUE + "of this package be? (Enter " + ChatColor.RED + "none" + ChatColor.BLUE + " for no name)";
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
            return ChatColor.BLUE + "What should the " + ChatColor.GOLD + "description " + ChatColor.BLUE + "of this package be? (Enter " + ChatColor.RED + "none" + ChatColor.BLUE + " for no description)";
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
            return ChatColor.BLUE + "Who or what should the " + ChatColor.GOLD + "creator " + ChatColor.BLUE + "of this package be? (Enter " + ChatColor.RED + "none" + ChatColor.BLUE + " for no creator)";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (!input.equalsIgnoreCase("none")) {
                ((PackageBuilder) context.getSessionData("builder")).withCreator(input);
            }
            Player player = (Player) context.getForWhom();
            if (player.hasPermission("redeem.createpackage.money") && RedeemMe.economy != null) {
                return new MoneyPrompt();
            } else if (player.hasPermission("redeem.createpackage.item")) {
                return new PreItemPrompt();
            } else if (player.hasPermission("redeem.createpackage.command")) {
                return new CommandPrompt();
            } else {
                context.getForWhom().sendRawMessage(ChatColor.RED + "Your permissions are screwed up. Aborting.");
                return Prompt.END_OF_CONVERSATION;
            }
        }
    }

    private class MoneyPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.BLUE + "How much " + ChatColor.GOLD + "money " + ChatColor.BLUE + "should this package have? (Enter " + ChatColor.RED + "0 " + ChatColor.BLUE + "for none)";
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
            Player player = (Player) context.getForWhom();
            if (player.hasPermission("redeem.createpackage.item")) {
                return new PreItemPrompt();
            } else if (player.hasPermission("redeem.createpackage.command")) {
                return new CommandPrompt();
            } else {
                return new EmbargoPrompt();
            }
        }
    }

    private class PreItemPrompt extends MessagePrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            if (context.getForWhom() instanceof Player) {
                Inventory inv = plugin.getServer().createInventory(null, 27, "Package Items");
                context.setSessionData("inventory", inv);
            }
            return ChatColor.BLUE + "Select " + ChatColor.GOLD + "items " + ChatColor.BLUE + "you wish to include in the package.";
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
            return ChatColor.BLUE + "Place the " + ChatColor.GOLD + "items " + ChatColor.BLUE + "you wish to include into the top half of the inventory screen. Type " + ChatColor.RED + "done" + ChatColor.BLUE + " when you are finished. Type anything else to show this message and open the inventory again. You are not requried to add any items.";
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
                Player player = (Player) context.getForWhom();
                if (player.hasPermission("redeem.createpackage.command")) {
                    return new CommandPrompt();
                } else {
                    return new EmbargoPrompt();
                }
            } else {
                return new ItemPrompt();
            }
        }

        @Override
        public boolean blocksForInput(ConversationContext context) {
            return true;
        }
    }

    private class CommandPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.BLUE + "Enter a " + ChatColor.GOLD + "command " + ChatColor.BLUE + "you wish this package to run. The string " + ChatColor.GOLD + "<player> " + ChatColor.BLUE + "will be replaced with the name of the player redeeming the package. Say " + ChatColor.RED + "done" + ChatColor.BLUE + " when you are finished. You are not required to enter any commands.";
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
            return ChatColor.BLUE + "Should the command " + ChatColor.GOLD + context.getSessionData("command") + ChatColor.BLUE + " be run as " + ChatColor.GOLD + "console" + ChatColor.BLUE + "?";
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
            return ChatColor.BLUE + "What should the " + ChatColor.GOLD + "embargo " + ChatColor.BLUE + "be set to for this package? Respond in the format yyyy-MM-dd HH:mm:ss. Say " + ChatColor.RED + "none" + ChatColor.BLUE + " for no embargo. The current time is " + ChatColor.GOLD + sdf.format(new Date());
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
            return ChatColor.BLUE + "What should the " + ChatColor.GOLD + "expiry " + ChatColor.BLUE + "be set to for this package? Respond in the format yyyy-MM-dd HH:mm:ss. Say " + ChatColor.RED + "none" + ChatColor.BLUE + " for no expiry.  The current time is " + ChatColor.GOLD + sdf.format(new Date());
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
            return ChatColor.BLUE + "Which " + ChatColor.GOLD + "server " + ChatColor.BLUE + "should this package be valid on? (Enter " + ChatColor.GOLD + "* " + ChatColor.BLUE + "for all servers)";
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
            return ChatColor.BLUE + "Should this package be for a " + ChatColor.GOLD + "player " + ChatColor.BLUE + "only, or should it have a " + ChatColor.GOLD + "coupon " + ChatColor.BLUE + "code?";
        }

        @Override
        protected boolean isInputValid(ConversationContext context, String input) {
            input = input.toLowerCase();
            return super.isInputValid(context, input);
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
            return ChatColor.BLUE + "Which " + ChatColor.GOLD + "player " + ChatColor.BLUE + "should be able to redeem this package?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            ((PackageBuilder) context.getSessionData("builder")).forPlayer(input);
            return new ReviewPrompt();
        }
    }

    private class CouponRemainingPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.GOLD + "How many " + ChatColor.BLUE + "coupons should be available? (Enter " + ChatColor.GOLD + "-1 " + ChatColor.BLUE + "for unlimited)";
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
            context.setSessionData("quantity", input.intValue());
            return new CouponTypePrompt();
        }
    }

    private class CouponTypePrompt extends BooleanPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.BLUE + "Would you like to use a " + ChatColor.GOLD + "custom " + ChatColor.BLUE + "coupon code?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            if (input) {
                return new CouponCodePrompt();
            } else {
                ((PackageBuilder) context.getSessionData("builder")).withCode(new CouponCode((Integer) context.getSessionData("quantity")));
                return new ReviewPrompt();
            }
        }
    }

    private class CouponCodePrompt extends RegexPrompt {

        public CouponCodePrompt() {
            super("[A-Za-z0-9]{15}");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.BLUE + "Enter a 15 character alphanumeric coupon code.";
        }

        @Override
        protected boolean isInputValid(ConversationContext context, String input) {
            return super.isInputValid(context, input.toUpperCase());
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return "Invalid coupon code";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {
            ((PackageBuilder) context.getSessionData("builder")).withCode(new CouponCode(input, (Integer) context.getSessionData("quantity")));
            return new ReviewPrompt();
        }
    }

    private class ReviewPrompt extends BooleanPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            try {
                for (String msg : ((PackageBuilder) context.getSessionData("builder")).details()) {
                    context.getForWhom().sendRawMessage(msg);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return ChatColor.BLUE + "Is this information correct?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            try {
                if (input) {
                    try {
                        PackageBuilder builder = (PackageBuilder) context.getSessionData("builder");
                        builder.build();
                        if (builder.getCode() != null) {
                            context.getForWhom().sendRawMessage(ChatColor.BLUE + "Sucessfully created coupon with code " + ChatColor.GOLD + builder.getCode() + ChatColor.BLUE + "!");
                        } else {
                            context.getForWhom().sendRawMessage(ChatColor.BLUE + "Sucessfully created package for " + ChatColor.GOLD + builder.getPlayer() + ChatColor.BLUE + "!");
                        }
                    } catch (SQLException e) {
                        context.getForWhom().sendRawMessage(ChatColor.RED + "There was a SQL error when creating the package. Aborting.");
                        plugin.getLogger().log(Level.SEVERE, "Error creating coupon", e);
                    } catch (CouponCodeAlreadyExistsException e) {
                        context.getForWhom().sendRawMessage(ChatColor.RED + "That coupon code already exists. Aborting.");
                    } catch (IncompletePackageException e) {
                        context.getForWhom().sendRawMessage(ChatColor.RED + "Your package is incomplete. Make sure you have money, items, or commands. Aborting.");
                    }
                } else {
                    context.getForWhom().sendRawMessage(ChatColor.RED + "Aborted package creation.");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return Prompt.END_OF_CONVERSATION;
        }

    }
}