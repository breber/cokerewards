using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using CookComputing.XmlRpc;

namespace CokeRewards
{
    [XmlRpcUrl("https://www.mycokerewards.com/xmlrpc")]
    public interface IGetPoints : IXmlRpcProxy
    {
        [XmlRpcMethod("points.pointsBalance")]
        string GetStateName(string emailAddress, string password, string screenName, string VERSION); 
    }
}
