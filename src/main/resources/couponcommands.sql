CREATE TABLE IF NOT EXISTS `couponcommands` (
  `id` int(11) NOT NULL,
  `command` varchar(150) NOT NULL,
  `console` tinyint(1) NOT NULL DEFAULT '1',
  KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;