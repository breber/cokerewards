﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Phone.Controls;

namespace CokeRewards
{
    public partial class MainPage : PhoneApplicationPage
    {
        // Constructor
        public MainPage()
        {
            InitializeComponent();

            if (/* !loggedIn*/ false)
            {
                NavigationService.Navigate(new Uri("/Login.xaml"));
            }
        }

        private void submitCodeButton_Click(object sender, RoutedEventArgs e)
        {
            string code = Code.Text;

            // TODO: remove this
            Console.Write("Code: " + code);
        }
    }
}