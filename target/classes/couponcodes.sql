CREATE TABLE IF NOT EXISTS `couponcodes` (
  `codeid` int(5) NOT NULL AUTO_INCREMENT,
  `code` varchar(15) NOT NULL,
  `remaining` int(5) NOT NULL,
  PRIMARY KEY (`codeid`),
  UNIQUE KEY `code` (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;